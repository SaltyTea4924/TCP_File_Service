package file_service;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class FileServer {
    public static void main(String[] args) throws Exception{
        int port = 3000;
        ServerSocketChannel welcomeChannel = ServerSocketChannel.open();
        welcomeChannel.socket().bind( new InetSocketAddress(port));
        while (true){
            SocketChannel serverSocket = welcomeChannel.accept();
            ByteBuffer request = ByteBuffer.allocate(2500);
            int numBytes = 0;
            do {
                numBytes = serverSocket.read(request);
            } while (numBytes >= 0);
            request.flip();
            char command = (char)request.get();
            //request.flip();
            System.out.println("received command: " + command);
            switch (command){
                case 'D': {
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileName = new String(a);
                    System.out.println("file to delete: "+fileName);
                    File file = new File("ServerFiles/"+fileName);
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
                    String oldName = "";
                    String newName = "";
                    String files = new String(a);

                    String[] fileNames = files.split("---");
                    oldName = fileNames[0];
                    newName = fileNames[1];
                    System.out.println(oldName);
                    System.out.println(newName);
                    File oldFile = new File("ServerFiles/"+oldName);
                    File newFile = new File("ServerFiles/"+newName);
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
                case 'L':
                    break;
                case 'G':
                    break;
                case 'U':
                    break;

            }
        }
    }
}
