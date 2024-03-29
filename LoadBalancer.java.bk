import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
// import java.util.Arrays;
import java.util.HashMap;
// import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class LoadBalancer {

    private static final String IP = "127.0.0.1";
    private static final int[] PORTS = { 2025, 2026, 2028 };
    static ServerSocket serverSocket;
    static HashMap<String, ArrayList<Integer>> messageList = new HashMap<>(); // Key: Files, Values: servers
    static int[] serverLoads = new int[PORTS.length];
    static AtomicReferenceArray<String> atomicMessages = new AtomicReferenceArray<String>(10);
    static AtomicInteger atomicCounter = new AtomicInteger(0);

    public static void main(String[] args) throws IOException {
        ArrayList<Integer> testData = new ArrayList<>();
        testData.add(1);
        messageList.put("m1", testData); // {"m1", [1]}
        // Arrays.fill(serverLoads, 0);
        serverLoads[0]++;
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
        ArrayList<Integer> putPorts = findLowestIndexes(serverLoads);
        // String[] emptyMessages = new String[10];
        // atomicMessages.set(emptyMessages);
        //

        public processReq(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            // putPorts.add(1);
            // putPorts.add(2);
            for (int i = 0; i < atomicMessages.length(); i++) {
                atomicMessages.getAndSet(i, null);
            }
            atomicCounter.set(0);
            System.out.println("Content of ports to upload message to: " + putPorts);
            try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true)) {

                String message = inFromClient.readLine();

                if (message.equals("client1") || message.equals("client2") || message.equals("client3")) {
                    ExecutorService executor = Executors.newFixedThreadPool(putPorts.size());

                    for (int port : putPorts) {
                        executor.submit(() -> pingServer(IP, PORTS[port])); // Ping servers concurrently
                        serverLoads[port]++;
                        messageList.put(message, putPorts);
                    }
                    ArrayList<Integer> testList = messageList.get(message);

                    System.out.println("content of the hashtable for new message: " + testList);

                    executor.shutdown();
                    executor.awaitTermination(5, TimeUnit.SECONDS);

                    outToClient.println(atomicMessages.get(0) + " " + atomicMessages.get(1)); // " " +
                                                                                              // atomicMessages.get(2)
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static ArrayList<Integer> findLowestIndexes(int[] array) {
            if (array == null || array.length == 0) {
                return new ArrayList<>();
            }

            int[] sortedArray = Arrays.copyOf(array, array.length);
            Arrays.sort(sortedArray);

            // Get the lowest value
            int lowestValue = sortedArray[0];

            // Find the first two indexes of the lowest value
            ArrayList<Integer> lowestIndexes = new ArrayList<>();
            int count = 0;
            for (int i = 0; i < array.length && count < 2; i++) {
                if (array[i] == lowestValue) {
                    lowestIndexes.add(i);
                    count++;
                }
            }
            if (lowestIndexes.size() != 2) {
                int test = lowestIndexes.get(0);
                test++;
                if (test > array.length - 1) {
                    test = 0;
                }
                lowestIndexes.add(test);
            }

            return lowestIndexes;
        }

    }
}
