import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ConnectException;
import java.util.Scanner;

public class Client {

    private static Socket clientSocket;
    private static DataOutputStream dos;
    private static DataInputStream dis;
    static String saveDirectory = "./client_received_files/";

    public void startConnection(String ip, int port) {

        try {
            clientSocket = new Socket(ip, port);
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendFile(String filepath) throws Exception {
        File file = new File(filepath);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[4 * 1024];
        long fileSize = file.length();

        try {
            dos.writeUTF("UPLOAD");
            dos.writeUTF(file.getName());
            // Send file size
            dos.writeLong(fileSize);

            // Send file content
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        } finally {
            fileInputStream.close();
        }
    }

    private static void getFile(String filename) throws Exception {
        dos.writeUTF("GET");
        dos.writeUTF(filename);

        FileOutputStream fos = new FileOutputStream(saveDirectory + filename);
        long fileSize = dis.readLong();
        byte[] buffer = new byte[4096];

        int bytesRead;
        while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
            fos.write(buffer, 0, bytesRead);
            fileSize -= bytesRead;
        }
        fos.close();
    }

    public static void deleteFile(String filename) throws Exception {
        dos.writeUTF("DELETE");
        dos.writeUTF(filename);
    }

    public static void stopConnection() {

        try {
            dos.close();
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("connection terminated");
    }

    public static void main(String[] args) {

        String[] serverAddresses = new String[] { "127.0.0.1", "127.0.0.1" };
        int[] portVals = new int[] { 2027, 2029 };
        Client client = new Client();
        Scanner scanner = new Scanner(System.in);
        System.out.println(
                "Enter your new command from the list: \nWrite UPLOAD <File name> to upload a file to the server\n" +
                        "Write GET <File name> to get receive a file from the server \nWrite DELETE <File name> to delete a file from the server \nWrite QUIT to exit");
        String command = scanner.nextLine();
        
        while (!command.equals("QUIT")) {
            int addressVal = 0;
            String[] parameters = command.split(" ");
            switch(parameters[0]){
                case "UPLOAD":
                    try{
                        clientSocket = new Socket(serverAddresses[addressVal], portVals[addressVal]);
                    }catch(ConnectException e){
                        System.out.println("Loadbalancer 1 failed");
                        addressVal = 1;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    
                    client.startConnection(serverAddresses[addressVal], portVals[addressVal]);
                    try {
                        sendFile(parameters[1]);
                        System.out.println("File successfully sent!");
                    }catch(ArrayIndexOutOfBoundsException e){
                        System.out.println("No file entered");
                    }catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    stopConnection();
                    
                break;
                case "DELETE":
                    try{
                        clientSocket = new Socket(serverAddresses[addressVal], portVals[addressVal]);
                    }catch(ConnectException e){
                        System.out.println("Loadbalancer 1 failed");
                        addressVal = 1;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    client.startConnection(serverAddresses[addressVal], portVals[addressVal]);
                    try {
                        deleteFile(parameters[1]);
                        System.out.println("File successfully deleted!");
                    }catch(ArrayIndexOutOfBoundsException e){
                        System.out.println("No file entered");
                    }catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    stopConnection();
                    
                break;
                case "GET":
                    try{
                        clientSocket = new Socket(serverAddresses[addressVal], portVals[addressVal]);
                    }catch(ConnectException e){
                        System.out.println("Loadbalancer 1 failed");
                        addressVal = 1;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    client.startConnection(serverAddresses[addressVal], portVals[addressVal]);
                    try {
                        getFile(parameters[1]);
                        System.out.println("File successfully received!");
                    }catch(ArrayIndexOutOfBoundsException e){
                        System.out.println("No file entered");
                    }catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    stopConnection();
                break;
                default:
                    System.out.println("Wrong request!\n");
                break;

            }
            System.out.println(
                    "Enter your new command from the list: \nWrite UPLOAD <File name> to upload a file to the server\n"
                            + "Write GET <File name> to get receive a file from the server \nWrite DELETE <File name> to delete a file from the server \nWrite QUIT to exit");
            command = scanner.nextLine();
        }
        scanner.close();

    }

}
