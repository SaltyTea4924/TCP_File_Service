package file_service;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
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
                case "D":
                    System.out.println("PLease enter file name");
                    String filename = keyboard.nextLine();
                    ByteBuffer request = ByteBuffer.wrap((command+filename).getBytes());
                    SocketChannel channel = SocketChannel.open();
                    channel.connect(new InetSocketAddress(args[0], serverPort));
                    channel.write(request);
                    channel.shutdownOutput();

                    ByteBuffer code = ByteBuffer.allocate(1);
                    channel.read(code);
                    code.flip();
                    byte[] a = new byte[STATUS_CODE_LENGTH];
                    code.get(a);
                    System.out.println(new String(a));
                    break;

                case "U":

                    break;

                case "G":

                    break;

                case "R":

                    break;

                case "L":

                    break;

                default:
                    if(!command.equals("Q")){
                        System.out.println("Invalid command!");
                    }
            }
        }while(!command.equals("Q"));
    }
}
