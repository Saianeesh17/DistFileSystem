import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    
    private static Socket clientSocket;
    private static DataOutputStream dos;
    private static DataInputStream dis;
    private String resp;
    static String saveDirectory = "./client_received_files/";

    public void startConnection(String ip, int port) {
        
        try{
            clientSocket = new Socket(ip, port);
            dos = new DataOutputStream(clientSocket.getOutputStream());
            dis = new DataInputStream(clientSocket.getInputStream());
        } catch (Exception e){
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

    private static void getFile(String filename) throws Exception{
        dos.writeUTF("GET");
        dos.writeUTF(filename);

        FileOutputStream fos = new FileOutputStream(saveDirectory + filename);
        long fileSize = dis.readLong();
        byte[] buffer = new byte[4096];

        int bytesRead;
        while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
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

        try{
            dos.close();
            clientSocket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("connection terminated");
    }

    public static void main(String[] args){
        String serverAddress = "127.0.0.1";
        Client client = new Client();
        client.startConnection(serverAddress, 2025);
        try {
            getFile("test.txt");
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //stopConnection();
        // System.out.println(response);
        client.startConnection(serverAddress, 2025);
        
        try {
            
            getFile("large.jpg");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        stopConnection();
    }
}
