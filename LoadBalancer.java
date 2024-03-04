import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class LoadBalancer {

    private static final String IP = "127.0.0.1";
    private static final int[] PORTS = {2025, 2026, 2028};
    static ServerSocket serverSocket;
    // Removed static socket, outToClient, inFromClient, messagesToClient

    static AtomicReferenceArray<String> atomicMessages =
            new AtomicReferenceArray<String>(10);
    static AtomicInteger atomicCounter = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {
        serverSocket = new ServerSocket(2027); // Create ServerSocket once
        System.out.println("Load balancer on port " + 2027);

        ExecutorService executor = Executors.newFixedThreadPool(PORTS.length);

        while (true) { // Continuously accept requests
            Socket socket = serverSocket.accept(); // Separate socket for each client
            System.out.println("Client connected");
            executor.submit(new processReq(socket)); // Handle requests in a separate thread
        }
    }

    private static void pingServer(String ip, int port) {
        Socket clientSocket = null;
        PrintWriter out = null;
        BufferedReader in = null;
        String response = null;
    
        try {
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    
            out.println("hello server"); // Send a message to initiate communication
            response = in.readLine();
    
            System.out.println("Response from server to LoadBalancer on port " + port + ": " + response);
            int index = atomicCounter.getAndIncrement();
            atomicMessages.set(index, response);
    
        } catch (IOException e) {
            System.err.println("Error pinging server to LoadBalncer on port " + port + ": " + e.getMessage());
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class processReq implements Runnable {
        Socket socket;

        public processReq(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true)) {

                String message = inFromClient.readLine();

                if (message.equals("client1")) {
                    ExecutorService executor = Executors.newFixedThreadPool(PORTS.length);

                    for (int port : PORTS) {
                        executor.submit(() -> pingServer(IP, port)); // Ping servers concurrently
                    }

                    executor.shutdown();
                    executor.awaitTermination(5, TimeUnit.SECONDS);

                    outToClient.println(atomicMessages.get(0) + " " + atomicMessages.get(1) + " " + atomicMessages.get(2));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
