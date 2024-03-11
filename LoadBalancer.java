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
        // PrintWriter out = null;
        DataOutputStream out = null;
        // BufferedReader in = null;
        DataInputStream in = null;
        String response = null;

        try {
            clientSocket = new Socket(ip, port);
            // out = new PrintWriter(clientSocket.getOutputStream(), true);
            // in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            out.println("hello server"); // Send a message to initiate communication
            // response = in.readLine();
            while (true){
                // String message = 
                // dataOutputStream.writeUTF(out.);
                System.out.print();
                if(message.equalsIgnoreCase("exit()"))
                    break;
            }

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
        // ArrayList<Integer> putPorts = findLowestIndexes(serverLoads);
        ArrayList<Integer> putPorts = new ArrayList<Integer>(3);
        // String[] emptyMessages = new String[10];
        // atomicMessages.set(emptyMessages);
        //

        public processReq(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            putPorts.add(0);
            putPorts.add(1);
            putPorts.add(2);
            // putPorts.add(1);
            // putPorts.add(2);
            for (int i = 0; i < atomicMessages.length(); i++) {
                atomicMessages.getAndSet(i, null);
            }
            atomicCounter.set(0);
            System.out.println("Content of ports to upload message to: " + putPorts);
            // try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //

            try (
                DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                FileOutputStream fileOutputStream = new FileOutputStream("large.jpg") // Replace with desired filename
            ) {
                // Receive file size
                long fileSize = in.readLong();
                System.out.println("Receiving file of size: " + fileSize + " bytes");

                // Receive file content
                byte[] buffer = new byte[4 * 1024];
                long totalBytesRead = 0;
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                    for (int p : PORTS){
                        Socket server_socket = new Socket(ip, p);
                        DataOutputStream data_to_server = new DataOutputStream(server_socket.getOutputStream());
                        data_to_server.write(buffer, 0, bytesRead);
                    }
                    totalBytesRead += bytesRead;

                    if (totalBytesRead == fileSize) {
                        break; // Break loop when all bytes received
                    }
                }

                System.out.println("File received successfully");
                out.writeUTF("File received successfully");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                clientSocket.close();
            }



            // try (DataInputStream inFromClient = new DataInputStream(new InputStreamReader(socket.getInputStream()));
            //         // PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true)) {
            //         DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream())) {

            //     // Take in message from the client

            //     // String message = inFromClient.readLine();
            //     String message;

            //     while (true){
            //         message = inFromClient.readUTF();
            //         System.out.println(message);
            //         if(message.equalsIgnoreCase("exit()")){
            //             break;
            //         }
            //     }

            //     

            //     // if (message.equals("client1") || message.equals("client2") || message.equals("client3")) {
            //     ExecutorService executor = Executors.newFixedThreadPool(putPorts.size());

            //     for (int port : putPorts) {
            //         executor.submit(() -> pingServer(IP, PORTS[port])); // Ping servers concurrently
            //         serverLoads[port]++;
            //         // messageList.put(message, putPorts);
            //     }
            //     ArrayList<Integer> testList = messageList.get(message);

            //     // System.out.println("content of the hashtable for new message: " + testList);

            //     executor.shutdown();
            //     executor.awaitTermination(5, TimeUnit.SECONDS);

            //     outToClient.println(atomicMessages.get(0) + " " + atomicMessages.get(1)+" " + atomicMessages.get(2)); // " " +
            //                                                                                   // atomicMessages.get(2)
            //     // }
            // } catch (Exception e) {
            //     e.printStackTrace();
            // }
        }

        // public static ArrayList<Integer> findLowestIndexes(int[] array) {
        //     if (array == null || array.length == 0) {
        //         return new ArrayList<>();
        //     }

        //     int[] sortedArray = Arrays.copyOf(array, array.length);
        //     Arrays.sort(sortedArray);

        //     // Get the lowest value
        //     int lowestValue = sortedArray[0];

        //     // Find the first two indexes of the lowest value
        //     ArrayList<Integer> lowestIndexes = new ArrayList<>();
        //     int count = 0;
        //     for (int i = 0; i < array.length && count < 2; i++) {
        //         if (array[i] == lowestValue) {
        //             lowestIndexes.add(i);
        //             count++;
        //         }
        //     }
        //     if (lowestIndexes.size() != 2) {
        //         int test = lowestIndexes.get(0);
        //         test++;
        //         if (test > array.length - 1) {
        //             test = 0;
        //         }
        //         lowestIndexes.add(test);
        //     }

        //     return lowestIndexes;
        // }

    }
}
