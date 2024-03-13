import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    
    private static Socket clientSocket;
    private static DataOutputStream out;
    private static DataInputStream in;
    private String resp;

    public void startConnection(String ip, int port) {
        
        try{
            clientSocket = new Socket(ip, port);
            out = new DataOutputStream(clientSocket.getOutputStream());
            in = new DataInputStream(clientSocket.getInputStream());
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
            out.writeUTF("UPLOAD");
            out.writeUTF(file.getName());
            // Send file size
            out.writeLong(fileSize);
    
            // Send file content
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            fileInputStream.close();
        }
    }
    

    public static void stopConnection() {

        try{
            out.close();
            clientSocket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("connection terminated");
    }

    public static void main(String[] args){
        String serverAddress = "127.0.0.1";
        Client client = new Client();
        client.startConnection(serverAddress, 2027);
        try {
            sendFile("large.jpg");
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        stopConnection();
        // System.out.println(response);
        client.startConnection(serverAddress, 2027);
        
        try {
            
            sendFile("test.txt");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        stopConnection();
    }
}
