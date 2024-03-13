import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class LoadBalancer {
    public static final int PORT = 2027;
    public static final int[] SERVER_PORTS = {2025, 2026, 2028};
    public static final String[] SERVER_HOSTS = {"localhost", "localhost", "localhost"};

    public static void main(String[] args) {
        // Start the server logic in a new thread
        new Thread(new ServerLogic()).start();
        Timer timer = new Timer();
        ReqStatus req = new ReqStatus();
        timer.schedule(req, 0, 5000);
        // Other operations can be performed here concurrently
    }

    public static class ServerLogic implements Runnable {
        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Load balancer running on port " + PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected");
                    // Create a new thread for each client connection
                    new Thread(new ProcessReq(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ProcessReq implements Runnable {
        private Socket clientSocket;

        public ProcessReq(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                String request = dis.readUTF();
                String filename = dis.readUTF();
                long filesize = dis.readLong();

                byte[] fileContent = new byte[(int) filesize];
                dis.readFully(fileContent);

                for (int i = 0; i < SERVER_PORTS.length; i++) {
                    Socket serverSocketConnection = new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
                    System.out.println("Connected to server on port " + SERVER_PORTS[i]);

                    DataOutputStream dos = new DataOutputStream(serverSocketConnection.getOutputStream());
                    dos.writeUTF(request);
                    dos.writeUTF(filename);
                    dos.writeLong(filesize);
                    dos.write(fileContent);

                    serverSocketConnection.close();
                }

                clientSocket.close();
                System.out.println("File transferred to all servers");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ReqStatus extends TimerTask{

        public static int leader = 0;

        static String[][] fileContents = new String[3][];

        @Override
        public void run() {
            // TODO Auto-generated method stub
            // System.out.println("Hello World");
            try {
                checkServerStatus();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public static void checkServerStatus() throws IOException {
            for (int i = 0; i < SERVER_PORTS.length; i++) {
                // Connect to each server
                Socket serverSocketConnection = new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
                System.out.println("Connected to server on port " + SERVER_PORTS[i]);

                // Transfer the filename, filesize, and file to the server
                DataOutputStream out = new DataOutputStream(serverSocketConnection.getOutputStream());
                DataInputStream in = new DataInputStream(serverSocketConnection.getInputStream());
                 // Write the file content to the server
                out.writeUTF("STATUS");
    
                // Receive and print the string array
                int arrayLength = in.readInt();
                String[] documentNames = new String[arrayLength];
                for (int j = 0; j < arrayLength; j++) {
                    documentNames[j] = in.readUTF();
                }

                fileContents[i] = Arrays.copyOf(documentNames, documentNames.length);
                     
                // Print the received array
                System.out.println("Documents on server:");
                for (String documentName : documentNames) {
                    System.out.println(documentName);
                } 

                    serverSocketConnection.close();
                }


                // if(!compareArrays(fileContents[0], fileContents[1], fileContents[2])){
                //     // detect the missing file(s)
                //     for (int i = 0; i < fileContents[0].length; i++){
                //         if (!fileContents[(leader + 1) % 3][i].equals(fileContents[leader][i])) {
                //             
                //         }
                //         if (!fileContents[(leader + 2) % 3][i].equals(fileContents[leader][i])) {
                //             
                //         }
                //     }
                // };
                
                // Start Here
                // static String[][] fileContents = new String[3][];
                
                // String[] arr1 = {
                //     "value1",
                //     "value2",
                // };
                // String[] arr2 = {"value1"};
                // String[] arr3 = {"value1", "value2", "value3"};
                // String[] arr4 = {"value1", "value3", "value4"};
                String[][] server_2_update = compareArrays(fileContents[0], fileContents[1]);
                String[][] server_3_update = compareArrays(fileContents[0], fileContents[2]);
            
        }

        public static boolean compareArrays(String[] array1, String[] array2, String[] array3) {
            // Check if the arrays have the same length
            if (array1.length != array2.length || array1.length != array3.length) {
                return false;
            }
    
            // Compare the values of corresponding elements in the arrays
            for (int i = 0; i < array1.length; i++) {
                if (!array1[i].equals(array2[i]) || !array1[i].equals(array3[i])) {
                    return false;
                }
            }
    
            // If all corresponding elements are equal, return true
            return true;
        }

    public static String[][] compareArrays(String[] base, String[] compare) {
        Set<String> baseSet = new HashSet<>();
        Set<String> compareSet = new HashSet<>();
        List<String> plusValues = new ArrayList<>();
        List<String> minusValues = new ArrayList<>();

        // Convert arrays to sets for easier comparison
        for (String val : base) {
            baseSet.add(val);
        }
        for (String val : compare) {
            compareSet.add(val);
        }

        // Check for elements in compareSet but not in baseSet (-)
        for (String val : compareSet) {
            if (!baseSet.contains(val)) {
                minusValues.add(val);
            }
        }

        // Check for elements in baseSet but not in compareSet (+)
        for (String val : baseSet) {
            if (!compareSet.contains(val)) {
                plusValues.add(val);
            }
        }

        String[][] result = new String[2][];
        result[0] = plusValues.toArray(new String[0]); // "+" values
        result[1] = minusValues.toArray(new String[0]); // "-" values

        return result;
    }

    }
}



