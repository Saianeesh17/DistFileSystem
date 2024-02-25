import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LoadBalancer {

    private static final String IP = "127.0.0.1";
    private static final int[] PORTS = {2025, 2026};

    public static void main(String[] args) {
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

            System.out.println("Response from server on port " + port + ": " + response);
        } catch (IOException e) {
            System.err.println("Error pinging server on port " + port + ": " + e.getMessage());
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