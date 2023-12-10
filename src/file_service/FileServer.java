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

public class FileServer {
    public static void main(String[] args) throws Exception{
        int port = 3000;
        ServerSocketChannel welcomeChannel = ServerSocketChannel.open();
        welcomeChannel.socket().bind( new InetSocketAddress(port));

        ExecutorService es = Executors.newFixedThreadPool(4);

        char command;

        do{

            SocketChannel serverSocket = welcomeChannel.accept();
            ByteBuffer request = ByteBuffer.allocate(2500);
            int numBytes;
            do {
                numBytes = serverSocket.read(request);
            } while ( request.position() > request.get() && numBytes >= 0);
            request.flip();
            command = (char)request.get();
            //request.flip();
            System.out.println("received command: " + command);
            switch (command){
                case 'D': {
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileName = new String(a);
                    fileName = fileName.replaceAll("\0", "");
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
                    break;
                }
                case 'R':{
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String oldName;
                    String newName;
                    String files = new String(a);

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
                    break;
                }
                case 'L':{
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
                    break;
                }
                case 'G':{
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileName = new String(a);
                    fileName = fileName.replaceAll("\0", "");
                    File file = new File("server_folder/"+fileName);
                    System.out.println(fileName);
                    if(!file.exists()){
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

                    while((fileLine = br.readLine()) != null){
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
                    break;
                }
                case 'U':{
                    int nameLength = request.getInt();
                    byte[] a = new byte[nameLength];
                    request.get(a);
                    FileOutputStream fs = new FileOutputStream("server_folder/" + new String(a), true);
                    FileChannel fc = fs.getChannel();

                    request.clear();
                    while(serverSocket.read(request) >= 0){
                        request.flip();
                        fc.write(request);
                        request.clear();
                    }
                    ByteBuffer code = ByteBuffer.wrap("S".getBytes());
                    serverSocket.write(code);

                    fs.close();

                    serverSocket.close();
                    break;
                } case 'Q': {
                    System.out.println("good bye");
                    es.shutdown();
                }

            }
        } while (command != 'Q');
    }
}
