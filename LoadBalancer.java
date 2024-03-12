import java.io.*;
import java.net.*;

public class LoadBalancer {

    private static final int PORT = 2027; // Replace with your desired port number
    // private static final String[] SERVER_ADDRESSES = {"localhost:8080", "localhost:8081", "localhost:8082"}; // Server addresses and ports
    private static final String[] SERVER_ADDRESSES = {"localhost:2025"};

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);

        while (true) {
            // Accept client connection
            Socket clientSocket = serverSocket.accept();

            // Read file information from client
            DataInputStream clientInput = new DataInputStream(clientSocket.getInputStream());
            String fileName = clientInput.readUTF();

            // Send file data to each server in separate threads
            for (String serverAddress : SERVER_ADDRESSES) {
                new Thread(() -> sendFileToServer(clientSocket, serverAddress, fileName)).start();
            }

            // No need to close client connection as data transfer happens within threads
        }
    }

    private static void sendFileToServer(Socket clientSocket, String serverAddress, String fileName) {
        try {
            // Extract server hostname and port
            String[] parts = serverAddress.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            // Open socket connection to server
            Socket serverSocket = new Socket(host, port);

            // Forward file data from client to server (replace with efficient transfer logic)
            DataOutputStream serverOutput = new DataOutputStream(serverSocket.getOutputStream());
            serverOutput.writeUTF(fileName);
            forwardStream(clientSocket.getInputStream(), serverOutput);

            // Close server connection
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void forwardStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
    }
}
