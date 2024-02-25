import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class LoadBalancer {

    private static final String IP = "127.0.0.1";
    private static final int[] PORTS = {2025, 2026};
    static ServerSocket serverSocket;
    static Socket socket;
    static PrintWriter outToClient;
    static BufferedReader inFromClient;
    static String[] messagesToClient = new String[10];
    static AtomicReferenceArray<String> atomicMessages = 
    new AtomicReferenceArray<String>(messagesToClient);
    // static int counter= 0;
    static AtomicInteger atomicCounter = 
    new AtomicInteger(0);

    public static void main(String[] args) {
        String message = null;
        try{
            serverSocket = new ServerSocket(2027);
            System.out.println("Load balancer on port " + 2027);
            socket = serverSocket.accept();
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            message = inFromClient.readLine();
            outToClient = new PrintWriter(socket.getOutputStream(), true);
        } catch (Exception e){
            e.printStackTrace();
        }

        if (message.equals("client1")){
            // System.out.println("hello");
            ExecutorService executor = Executors.newFixedThreadPool(PORTS.length);

            for (int port : PORTS) {
                executor.submit(() -> pingServer(IP, port)); // pings all the ports that are defined
            }

            executor.shutdown(); // Signal completion
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS); // Wait for tasks to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        outToClient.println(atomicMessages.get(0) + " " + atomicMessages.get(1));
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
            System.out.println(index);
            
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
}