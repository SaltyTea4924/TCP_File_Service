package file_service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileClient {
    private final static int STATUS_CODE_LENGTH = 1;
    public static ReentrantLock lock = new ReentrantLock();
    public static boolean isDownloading = false;
    public static boolean isUploading = false;
    public static Condition doneDownloading = lock.newCondition();
    public static Condition doneUploading = lock.newCondition();
    public static void main(String[] args) throws Exception{
        if (args.length != 2){
            System.out.println("Syntax: File Client <ServerIP> <ServerPort>");
            return;
        }
        ExecutorService es = Executors.newFixedThreadPool(4);
        int serverPort = Integer.parseInt(args[1]);
        String command;
        File folder = new File("client_folder");
        if (!folder.exists()) {
            folder.mkdirs();}
        do{
            System.out.println("Please type a command:");
            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toUpperCase();
            Future<String> result;
            switch (command){
                case "D": {
                    System.out.println("Please enter file name");
                    String filename = keyboard.nextLine();
                    ByteBuffer request = ByteBuffer.wrap((command + filename).getBytes(StandardCharsets.UTF_8));
                    //ByteBuffer request = ByteBuffer.wrap((command + filename).getBytes());
                    SocketChannel channel = SocketChannel.open();
                    channel.connect(new InetSocketAddress(args[0], serverPort));
                    channel.write(request);
                    channel.shutdownOutput();

                    ByteBuffer code = ByteBuffer.allocate(1);
                    channel.read(code);
                    channel.close();
                    code.flip();
                    byte[] a = new byte[STATUS_CODE_LENGTH];
                    code.get(a);
                    System.out.println(new String(a));
                    break;
                }

                case "U": {
                    //Can not run if the file is already being deleted, renamed, or uploading in the process
                    System.out.println("Please enter file name");
                    String fileName = keyboard.nextLine();
                    File file = new File("client_folder/"+fileName);
                    if (!file.exists()){
                        System.out.println("File doesn't exist!");
                        return;
                    }
                    es.submit(new UploadWorker(args, serverPort,command, fileName, file));
                    System.out.println("uploaded");
                    break;
                }
                case "G": {
                    //Can not run if the file is already being deleted, renamed, or uploading in the process
                    // can work with multiple download threads
                    System.out.println("Please enter file name");
                    String filename = keyboard.nextLine();
                    es.submit(new downloadWorker(args, serverPort,command, filename));

                    break;
                }
                case "R": {
                    System.out.println("Please enter file name");
                    String oldName = keyboard.nextLine();
                    System.out.println("Please enter in new file name to replace old name");
                    String newName = keyboard.nextLine();
                    ByteBuffer rename = ByteBuffer.wrap((command + oldName + "---" + newName).getBytes(StandardCharsets.UTF_8));
                    SocketChannel socket = SocketChannel.open();
                    socket.connect(new InetSocketAddress(args[0], serverPort));
                    socket.write(rename);
                    socket.shutdownOutput();

                    ByteBuffer c = ByteBuffer.allocate(1);
                    socket.read(c);
                    socket.close();
                    c.flip();
                    byte[] b = new byte[STATUS_CODE_LENGTH];
                    c.get(b);
                    System.out.println(new String(b));
                    break;
                }

                case "L":
                    ByteBuffer list = ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8));
                    SocketChannel chan = SocketChannel.open();
                    chan.connect(new InetSocketAddress(args[0], serverPort));
                    chan.write(list);
                    chan.shutdownOutput();

                    ByteBuffer bytes = ByteBuffer.allocate(2500);
                    int num = chan.read(bytes);
                    chan.close();
                    bytes.flip();
                    byte[] l = new byte[num];
                    bytes.get(l);
                    System.out.println(new String(l));
                    chan.close();
                    break;

                case "Q":
                    ByteBuffer quit = ByteBuffer.wrap(command.getBytes(StandardCharsets.UTF_8));
                    SocketChannel quitChannel = SocketChannel.open();
                    quitChannel.connect(new InetSocketAddress(args[0], serverPort));
                    quitChannel.write(quit);
                    quitChannel.shutdownOutput();
                    System.out.println("Thank you for using our file service.");
                    es.shutdown();

                default:
                    if(!command.equals("Q")){
                        System.out.println("Invalid command!");
                    }
            }
        }while(!command.equals("Q"));
    }
    static class UploadWorker implements Runnable {
        Scanner keyboard = new Scanner(System.in);
        String command;
        int serverPort;
        String[] args;
        String fileName;
        File file;
        public UploadWorker(String[] args, int serverport, String command, String fileName, File file){
            this.serverPort = serverport;
            this.args = args;
            this.command = command;
            this.fileName = fileName;
            this.file = file;


        }
        public void run(){
            try{
                while (isUploading == true) {
                    doneUploading.await();
                }
                isUploading = true;
                ByteBuffer request = ByteBuffer.allocate(2000);
                request.put(command.getBytes());
                request.putInt(fileName.length());
                request.put(fileName.getBytes());
                FileInputStream fs = new FileInputStream(file);
                FileChannel fc = fs.getChannel();

                SocketChannel channel = SocketChannel.open();
                channel.connect(new InetSocketAddress(args[0], serverPort));
                do {
                    request.flip();
                    channel.write(request);
                    request.clear();
                }while (fc.read(request)>=0);
                channel.shutdownOutput();
                ByteBuffer code = ByteBuffer.allocate(1);
                channel.read(code);
                channel.close();
                code.flip();
                byte[] a = new byte[STATUS_CODE_LENGTH];
                code.get(a);
                System.out.println(new String(a));
                isUploading = false;
                doneUploading.signal();
                //TODO: receive the status code from server
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    static class downloadWorker implements Runnable{
        Scanner keyboard;
        String command;
        int serverPort;
        String filename;
        String[] args;
        public downloadWorker(String[] args, int serverport, String command, String filename){
            this.serverPort = serverport;
            this.args = args;
            this.command = command;
            this.filename = filename;
        }
        public void run(){
            lock.lock();
            try{
                while (isDownloading == true || isUploading == true){
                    doneDownloading.await();
                }
                isDownloading = true;
                ByteBuffer request = ByteBuffer.wrap((command + filename).getBytes(StandardCharsets.UTF_8));
                SocketChannel channel = SocketChannel.open();
                channel.connect(new InetSocketAddress(args[0], serverPort));
                channel.write(request);
                channel.shutdownOutput();


                 ByteBuffer code = ByteBuffer.allocate(2500);
                 channel.read(code);
                 code.flip();

                 File file = new File("client_folder/" + filename);
                   if (file.exists()){
                     System.out.println("File already exists");
                     return;
            }
                 ByteBuffer buffer = ByteBuffer.allocate(1024);
                   try (FileOutputStream fileOutputStream = new FileOutputStream("client_folder/" + filename)) {
                     int bytesRead;
                       while ((bytesRead = channel.read(buffer)) > 0) {
                           buffer.flip();
                           fileOutputStream.write(buffer.array(), 0, bytesRead);
                           buffer.clear();
                       }
                 }

                  channel.close();
                  byte[] a = new byte[STATUS_CODE_LENGTH];
                  buffer.get(a);
                  System.out.println(new String(a));
                  isDownloading = false;
                  doneDownloading.signal();
              } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }
}
