import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.ConnectException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.io.IOException;

public class Client {

    private static Socket clientSocket;
    private static DataOutputStream dos;
    private static DataInputStream dis;
    static String saveDirectory = "./client_received_files/";

    //Starts the connection to the load balancer by initializing the socket and its input and output streams 
    public void startConnection(String ip, int port) {

        try {
            clientSocket = new Socket(ip, port);
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // sendFile method sends a file object over the data output stream created in startConnection
    public static void sendFile(String filepath) throws Exception {
        File file = new File(filepath);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[4 * 1024];
        long fileSize = file.length();

        try {
            // write the request and file's name to the server
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

    // getFile reads a file over the data input stream (dis) and writes it locally
    private static void getFile(String filename) throws Exception {
        // write the request and file's name to the server
        dos.writeUTF("GET");
        dos.writeUTF(filename);
        try{
            long fileSize = readWithTimeout(dis, 4000);
            // Create a FileOutputStream to write the file to /client_received_files
            FileOutputStream fos = new FileOutputStream(saveDirectory + filename);
            byte[] buffer = new byte[4096];
        
            int bytesRead;
            // write the file
            while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize))) != -1) {
                fos.write(buffer, 0, bytesRead);
                fileSize -= bytesRead;
            }
            fos.close();
            //alert user that file has been successfully loaded
            System.out.println("File successfully received!");
        }catch(TimeoutException e){
            //alert user that file is not on any servers
            System.out.println("File does not exist");
        }
        
    }

         // readWithTimeout allows us to read from an input stream, within an alotted amount of time
    public static long readWithTimeout(DataInputStream in, long timeoutMillis) throws IOException, TimeoutException {
        // Record the starting time
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            // Check if any data is available for reading
            if (in.available() > 0) {
            return in.readLong();
            }
            try {
            Thread.sleep(10); 
            } catch (InterruptedException e) {
            }
        }
        // Timeout reached, throw an exception 
        throw new TimeoutException("No data received within timeout");
        }

    // deleteFile allows us to remove files on the server
    public static void deleteFile(String filename) throws Exception {
        // write the request and file's name to the server
        dos.writeUTF("DELETE");
        dos.writeUTF(filename);
    }

    // simply terminates the connection between client and load balancer
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
        // Create the Client object and Scanner for user to input requets
        Client client = new Client();

        Scanner scanner = new Scanner(System.in);
        //prompt user for requests
        System.out.println(
                "Enter your new command from the list: \nWrite UPLOAD <File name> to upload a file to the server\n" +
                        "Write GET <File name> to get receive a file from the server \nWrite DELETE <File name> to delete a file from the server \nWrite QUIT to exit");
        
        // Read user requests and process commands
        String command = scanner.nextLine();
        
        while (!command.equals("QUIT")) {
            int addressVal = 0;
            String[] parameters = command.split(" ");
            switch(parameters[0]){
                case "UPLOAD":
                    boolean checkFile = false;
                    try{
                        // Attempt to establish a socket connection to load balancer
                        clientSocket = new Socket(serverAddresses[addressVal], portVals[addressVal]);
                    }catch(ConnectException e){
                        System.out.println("Loadbalancer 1 failed");
                        addressVal = 1;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    
                    client.startConnection(serverAddresses[addressVal], portVals[addressVal]);
                    try {
                        // Check to make sure the already file exists on the server. If it does, the upload is rejected. If it doesn't exist on local device to upload to server, alert user. Else, let user know file upload was successful.
                        File directory = new File(System.getProperty("user.dir"));
                        File[] files = directory.listFiles();
                        if (files != null) { 
                            for (File file : files) { 
                                if(file.getName().equals(parameters[1])){
                                    checkFile = true;
                                }
                            } 
                        }
                        if(checkFile){
                            sendFile(parameters[1]);
                        }else{
                            throw new Exception("");
                        }
                        
                        System.out.println("File successfully sent!");
                    }catch(ArrayIndexOutOfBoundsException e){
                        System.out.println("No file entered");
                    }catch (Exception e) {
                        System.out.println("File does not exist");
                    }
                    stopConnection();
                    
                break;
                case "DELETE":
                    
                    try{
                        //connect to load balancer
                        clientSocket = new Socket(serverAddresses[addressVal], portVals[addressVal]);
                    }catch(ConnectException e){
                        System.out.println("Loadbalancer 1 failed");
                        addressVal = 1;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    client.startConnection(serverAddresses[addressVal], portVals[addressVal]);
                    try {
                        // Send delete command for the specified file
                        deleteFile(parameters[1]);
                        System.out.println("File successfully deleted!");
                    }catch(ArrayIndexOutOfBoundsException e){
                        System.out.println("No file entered");
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    stopConnection();
                    
                break;
                case "GET":
                
                    try{
                        //connect to load balancer
                        clientSocket = new Socket(serverAddresses[addressVal], portVals[addressVal]);
                    }catch(ConnectException e){
                        System.out.println("Loadbalancer 1 failed");
                        addressVal = 1;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    client.startConnection(serverAddresses[addressVal], portVals[addressVal]);
                    try {
                        // Request the specified file from the server
                        getFile(parameters[1]);
                        
                    }catch(ArrayIndexOutOfBoundsException e){
                        System.out.println("No file entered");
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    stopConnection();
                break;
                default:
                    System.out.println("Wrong request!\n");
                break;

            }
            // Prompt to get next request from user
            System.out.println(
                    "Enter your new command from the list: \nWrite UPLOAD <File name> to upload a file to the server\n"
                            + "Write GET <File name> to get receive a file from the server \nWrite DELETE <File name> to delete a file from the server \nWrite QUIT to exit");
            command = scanner.nextLine();
        }
        scanner.close();

    }
}
