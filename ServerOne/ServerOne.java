package ServerOne;
import java.io.*;
import java.net.*;

public class ServerOne{

    int port = 2025;
    Socket server;
    ServerSocket serverSocket;
    PrintWriter out;
    BufferedReader in;
    String saveDirectory = "./received_files/";
    
    public ServerOne(){
        
        
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(this.port);
            System.out.println("Server listening on port" + port);
            while(true){
                server = serverSocket.accept();
                DataInputStream dis = new DataInputStream(server.getInputStream());
                
                String filePath = saveDirectory + "large.jpg";
                
                FileOutputStream fos = new FileOutputStream(filePath);

                long fileSize = dis.readLong();
                byte[] buffer = new byte[4096];

                int bytesRead;
                while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    fileSize -= bytesRead;
                }

                fos.close();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    

    public static void main(String[] args){
        // System.out.println("Hello World");
        ServerOne serverOne =  new ServerOne();
        serverOne.startServer();
    }
}
