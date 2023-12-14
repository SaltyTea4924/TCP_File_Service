package file_service;

import java.io.*;
import java.net.InetSocketAddress;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileServer {

    public static ReentrantLock lock = new ReentrantLock();
    public static Condition free = lock.newCondition();
    public static boolean downloading = false;
    public static boolean uploading = false;
    public static ByteBuffer request;
    public static SocketChannel serverSocket;
    public static Character command;
    public static byte[] a;
    public static void main(String[] args) throws Exception{
        int port = 3000;

        ExecutorService es = Executors.newFixedThreadPool(4);

        try (ServerSocketChannel welcomeChannel = ServerSocketChannel.open()) {
            welcomeChannel.socket().bind(new InetSocketAddress(port));
            request = ByteBuffer.allocate(2500);

            while (true) {
                try {
                    serverSocket = welcomeChannel.accept();
                    int numBytes;
                    do {
                        numBytes = serverSocket.read(request);
                    } while ( request.position() > request.get() && numBytes >= 0);
                    request.flip();
                    command = (char)request.get();
                    //request.compact();
                    System.out.println(command);
                    System.out.println(request);
                    //request.compact();
                    //request.flip();
                    System.out.println("received command: " + command);
                    //a = new byte[request.remaining()];

                    switch (command) {
                        case 'L':
                            System.out.println("listing files...");
                            listFiles();
                            System.out.println("files listed");
                            request.clear();
                            System.out.println("request cleared");
                            break;
                        case 'D':
                            System.out.println("deleting file... ");
                            deleteFile();
                            System.out.println("file deleted");
                            request.clear();
                            System.out.println("request cleared");
                            break;
                        case 'R':
                            System.out.println("renaming file...");
                            renameFile();
                            System.out.println("file renamed");
                            request.clear();
                            System.out.println("request cleared");
                            break;
                        case 'G':
                            es.submit(new Download());
                            break;
                        case 'U':
                            es.submit(new Upload());
                            break;
                        case 'Q':
                            System.out.println("good bye");
                            es.shutdown();
                            return;
                        default:
                            System.out.println("Invalid command");
                            ByteBuffer code = ByteBuffer.wrap("F".getBytes());
                            serverSocket.write(code);
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readCommand() throws IOException {
        int numBytes;
        do {
            numBytes = serverSocket.read(request);
        } while (request.position() > request.remaining() && numBytes >= 0);

        request.flip();

        // Extract the command from the buffer
        command = (char) request.get();

        // Compact the buffer to remove the read data
        request.compact();

        System.out.println("received command: " + command);
    }



    static class Other implements Runnable{
        @Override
        public void run() {
            try {
                lock.lock();

                switch (command) {
                    case 'D': {
                        System.out.println("deleting file... ");
                        deleteFile();
                        System.out.println("file deleted");
                        break;
                    }
                    case 'L': {
                        System.out.println("listing files...");
                        listFiles();
                        System.out.println("files listed");
                        break;
                    }
                    case 'R': {
                        System.out.println("renaming file...");
                        renameFile();
                        System.out.println("file renamed");
                        break;
                    } default: {
                        System.out.println("Invalid command");
                        ByteBuffer code = ByteBuffer.wrap("F".getBytes());
                        serverSocket.write(code);
                        break;
                    }
                }
            } catch(IOException e){
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }

    public static void deleteFile() throws IOException {
        byte[] a = new byte[request.remaining()];
        request.get(a);
        String fileName = new String(a);
        System.out.println(a);
        fileName = fileName.replaceAll("\0", "");
        System.out.println(fileName);
        System.out.println("file to delete: " + fileName + "00");
        File file = new File("server_folder/" + fileName);
        boolean success = false;
        if (file.exists()) {
            success = file.delete();
        }
        if (success) {
            ByteBuffer code = ByteBuffer.wrap("S".getBytes());
            serverSocket.write(code);
        } else {
            ByteBuffer code = ByteBuffer.wrap("F".getBytes());
            serverSocket.write(code);
        }
        serverSocket.close();
    }

    public static void renameFile() throws IOException {
        byte[] a = new byte[request.remaining()];
        request.get(a);
        String oldName;
        String newName;
        String files = new String(a);
        System.out.println(a);

        String[] fileNames = files.split("---");
        oldName = fileNames[0];
        newName = fileNames[1];
        newName = newName.replaceAll("\0", "");
        System.out.println(oldName);
        System.out.println(newName);
        File oldFile = new File("server_folder/"+oldName);
        File newFile = new File("server_folder/"+newName);
        boolean success = false;
        if (oldFile.exists()){
            success = oldFile.renameTo(newFile);
        }
        if (success) {
            ByteBuffer code = ByteBuffer.wrap("S".getBytes());
            serverSocket.write(code);
        } else {
            ByteBuffer code = ByteBuffer.wrap("F".getBytes());
            serverSocket.write(code);
        }
        serverSocket.close();
    }

    public static void listFiles() throws IOException {
        File[] files = new File("server_folder/").listFiles();

        String allFiles = "";

        String fileName;

        assert files != null;
        for (File file : files){
            if(file.isFile()){
                fileName = file.getName();
                allFiles = allFiles + fileName + "\n";

            }
        }

        System.out.println(allFiles);

        ByteBuffer list = ByteBuffer.wrap(allFiles.getBytes());
        serverSocket.write(list);

        serverSocket.close();
    }

    static class Download implements Runnable {
        public void run() {
            lock.lock();
            try {
                while(downloading || uploading){
                    free.await();
                }
                downloading = true;
                byte[] a = new byte[request.remaining()];
                request.get(a);
                String fileName = new String(a);
                fileName = fileName.replaceAll("\0", "");
                File file = new File("server_folder/" + fileName);
                System.out.println(fileName);
                if (!file.exists()) {
                    System.out.println("File does not exist!");
                    ByteBuffer code = ByteBuffer.wrap("F".getBytes());
                    serverSocket.write(code);
                }
                BufferedReader br = new BufferedReader(new FileReader(file));

                //System.out.println(fileLine);

                ByteBuffer c = ByteBuffer.allocate(2000);
                c.putInt(fileName.length());
                c.put(fileName.getBytes());
                serverSocket.write(c);
                c.clear();
                String fileLine;

                while ((fileLine = br.readLine()) != null) {
                    c.put(fileLine.getBytes());
                    c.put((byte) '\n');
                    c.flip();
                    serverSocket.write(c);
                    //System.out.println(c);
                    c.clear();

                }

                ByteBuffer code = ByteBuffer.wrap("S".getBytes());
                serverSocket.write(code);

                serverSocket.close();
                downloading = false;
                free.signal();
            } catch (IOException i){
                i.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
            request.clear();
            System.out.println("request cleared");
        }
    }

    public static class Upload implements Runnable {
        public void run() {
            lock.lock();
            try {
                while (downloading || uploading) {
                    free.await();
                }
                uploading = true;
                int nameLength = request.getInt();
                byte[] a = new byte[nameLength];
                request.get(a);
                FileOutputStream fs = new FileOutputStream("server_folder/" + new String(a), true);
                FileChannel fc = fs.getChannel();

                //request.clear();
                while (serverSocket.read(request) >= 0) {
                    request.flip();
                    fc.write(request);
                    request.clear();
                }
                ByteBuffer code = ByteBuffer.wrap("S".getBytes());
                serverSocket.write(code);

                fs.close();

                serverSocket.close();
                uploading = false;
                free.signal();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }

            request.clear();
            System.out.println("request cleared");
        }
    }
}
