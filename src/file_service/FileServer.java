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
            char command = (char)request.get();
            System.out.println("received command: " + command);
            switch (command){
                case 'D': {
                    byte[] a = new byte[request.remaining()];
                    request.get(a);
                    String fileName = new String(a);
                    File file = new File(fileName);
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
                    for ( int i = 0; i <= files.length(); i++){
                        int k = 0;
                        while(files.charAt(k) != '*'){
                            oldName = oldName + files.charAt(i);
                            k = k + 1;

                        }
                        i = i + k;
                        if (files.charAt(i) != '*') {
                            newName = newName + files.charAt(i);
                        }
                    }

                    File oldFile = new File(oldName);
                    File newFile = new File(newName);
                    boolean success = false;
                    if (oldFile.exists()){
                        success = newFile.renameTo(newFile);
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
