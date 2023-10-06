package file_service;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class FileClient {
    private final static int STATUS_CODE_LENGTH = 1;
    public static void main(String[] args) throws Exception{
        if (args.length != 2){
            System.out.println("Syntax: File Client <ServerIP> <ServerPort>");
            return;
        }
        int serverPort = Integer.parseInt(args[1]);
        String command;
        do{
            System.out.println("Please type a command:");
            Scanner keyboard = new Scanner(System.in);
            command = keyboard.nextLine().toUpperCase();
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
                    System.out.println("Please enter file name");
                    String fileName = keyboard.nextLine();
                    File file = new File("client_folder/"+fileName);
                    if (!file.exists()){
                        System.out.println("File doesn't exist!");
                        break;
                    }
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
                    channel.close();
                    //TODO: recieve the status code from server
                    break;
                }
                case "G": {
                    System.out.println("Please enter file name");
                    String filename = keyboard.nextLine();
                    ByteBuffer request = ByteBuffer.wrap((command + filename).getBytes(StandardCharsets.UTF_8));
                    SocketChannel channel = SocketChannel.open();
                    channel.connect(new InetSocketAddress(args[0], serverPort));
                    channel.write(request);
                    channel.shutdownOutput();

                    ByteBuffer code = ByteBuffer.allocate(2500);
                    channel.read(code);
                    channel.close();
                    code.flip();
                    byte[] a = new byte[STATUS_CODE_LENGTH];
                    code.get(a);
                    File file = new File("client_folder" + new String(a));
                    if (!file.exists()){
                        file.createNewFile();
                    }
                    System.out.println(new String(a));
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
                    chan.read(bytes);
                    chan.close();
                    bytes.flip();
                    byte[] l = new byte[bytes.toString().length()];
                    bytes.get(l);
                    System.out.println(new String(l));
                    chan.close();
                    break;

                default:
                    if(!command.equals("Q")){
                        System.out.println("Invalid command!");
                    }
            }
        }while(!command.equals("Q"));
    }
}
