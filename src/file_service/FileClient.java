package file_service;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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

                case "U":

                    break;

                case "G":

                    break;

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
                    bytes.flip();
                    byte[] l = new byte[bytes.toString().length()];
                    System.out.println(new String(l));
                    break;

                default:
                    if(!command.equals("Q")){
                        System.out.println("Invalid command!");
                    }
            }
        }while(!command.equals("Q"));
    }
}
