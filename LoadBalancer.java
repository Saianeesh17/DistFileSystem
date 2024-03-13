import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;

public class LoadBalancer {
    public static final int PORT = 2027;
    public static final int[] SERVER_PORTS = {2025, 2026, 2028};
    public static final String[] SERVER_HOSTS = {"localhost", "localhost", "localhost"};

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket;
        serverSocket = new ServerSocket(PORT);
        System.out.println("Load balancer running on port " + PORT);
        Thread t = new Thread(new ProcessReq(serverSocket));
        t.start();
        Timer timer = new Timer();
        reqStatus task = new reqStatus();        
        timer.schedule(task, 0, 5000);
    }

    public static class ProcessReq implements Runnable{
        ServerSocket serverSocket;
        public ProcessReq(ServerSocket serverSocket){
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            try {
                
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected");
        
                    // Accept filename, filesize, and file from the client
                    DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                    String request = dis.readUTF();
                    String filename = dis.readUTF();
                    long filesize = dis.readLong();
    
                    byte[] fileContent = new byte[(int) filesize];
                    dis.readFully(fileContent); // Read the file content into memory
    
                    for (int i = 0; i < SERVER_PORTS.length; i++) {
                        // Connect to each server
                        Socket serverSocketConnection = new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
                        System.out.println("Connected to server on port " + SERVER_PORTS[i]);
    
                        // Transfer the filename, filesize, and file to the server
                        DataOutputStream dos = new DataOutputStream(serverSocketConnection.getOutputStream());
                        dos.writeUTF(request);
                        dos.writeUTF(filename);
                        dos.writeLong(filesize);
                        dos.write(fileContent); // Write the file content to the server
    
                        serverSocketConnection.close();
                    }
    
                    clientSocket.close();
                    System.out.println("File transferred to all servers");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
        }
    
    }

    public static class reqStatus extends TimerTask{

        @Override
        public void run() {
            // TODO Auto-generated method stub
            System.out.println("Hello World");
        }

    }
}


