import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.*;

public class LoadBalancerTwo {

  private static final String IP = "127.0.0.1";
  private static final int[] PORTS = {2025, 2026, 2028};
  static ServerSocket serverSocket;
  static int[] serverLoads = new int[PORTS.length];
  
  // Fixed thread pool for sending data to servers
  private static final ExecutorService executorService = Executors.newFixedThreadPool(PORTS.length);

  public static void main(String[] args) throws IOException {
    serverSocket = new ServerSocket(2027);
    System.out.println("Load balancer on port " + 2027);

    while (true) {
      Socket socket = serverSocket.accept();
      System.out.println("Client connected");
      executorService.submit(new processReq(socket));
    }
  }

  static class processReq implements Runnable {
    Socket socket;

    public processReq(Socket socket) {
      this.socket = socket;
    }

    public void run() {
      try (
          DataInputStream in = new DataInputStream(socket.getInputStream());
          DataOutputStream out = new DataOutputStream(socket.getOutputStream())
      ) {
        // Receive file size
        long fileSize = in.readLong();
        System.out.println("Receiving file of size: " + fileSize + " bytes");

        // Receive file content in chunks
        byte[] buffer = new byte[4 * 1024];
        long totalBytesRead = 0;
        int bytesRead;

        ArrayList<Socket> serverSockets = new ArrayList<>();
        ArrayList<DataOutputStream> dataToServers = new ArrayList<>();

        // Open connections to all servers
        for (int p : PORTS) {
          serverSockets.add(new Socket(IP, p));
          dataToServers.add(new DataOutputStream(serverSockets.get(p).getOutputStream()));
        }

        while ((bytesRead = in.read(buffer)) != -1) {
          totalBytesRead += bytesRead;

          // Send data chunk to all servers concurrently
          for (int i = 0; i < PORTS.length; i++) {
            dataToServers.get(i).write(buffer, 0, bytesRead);
          }

          if (totalBytesRead == fileSize) {
            break;
          }
        }

        // Close server connections
        for (Socket serverSocket : serverSockets) {
          serverSocket.close();
        }

        System.out.println("File received successfully");
        out.writeUTF("File received successfully");

      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
