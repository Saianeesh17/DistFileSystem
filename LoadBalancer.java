import java.io.*;
import java.net.*;

public class LoadBalancer {
    private static final int PORT = 2027;
    private static final int[] SERVER_PORTS = {2025, 2026, 2028};
    private static final String[] SERVER_HOSTS = {"localhost", "localhost", "localhost"};

    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Load balancer running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");

                // Accept filename, filesize, and file from the client
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
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
