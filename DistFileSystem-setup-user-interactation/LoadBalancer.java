import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class LoadBalancer {
  public static final int PORT = 2027;
  public static final int[] SERVER_PORTS = {2025, 2026, 2028};
  public static final String[] SERVER_HOSTS = {"localhost", "localhost",
                                               "localhost"};
  public static boolean[] isRunning = {true, true, true};

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

    public ProcessReq(Socket clientSocket) { this.clientSocket = clientSocket; }

    @Override
    public void run() {
      try {
        DataInputStream dis =
            new DataInputStream(clientSocket.getInputStream());
        String request = dis.readUTF();
        // System.out.println(request);
        switch (request) {
          case "UPLOAD":
            String filename = dis.readUTF();
            long filesize = dis.readLong();
    
            byte[] fileContent = new byte[(int)filesize];
            dis.readFully(fileContent);
    
            for (int i = 0; i < SERVER_PORTS.length; i++) {
              Socket serverSocketConnection =
                  new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
              System.out.println("Connected to server on port " + SERVER_PORTS[i]);
    
              DataOutputStream dos =
                  new DataOutputStream(serverSocketConnection.getOutputStream());
              dos.writeUTF(request);
              dos.writeUTF(filename);
              dos.writeLong(filesize);
              dos.write(fileContent);
    
              serverSocketConnection.close();
            }
    
            clientSocket.close();
            break;
          
          case "DELETE":
            String deletename = dis.readUTF();
            for (int i = 0; i < SERVER_PORTS.length; i++) {
              Socket serverSocketConnection =
                  new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
              System.out.println("Connected to server on port " + SERVER_PORTS[i]);
    
              DataOutputStream dos =
                  new DataOutputStream(serverSocketConnection.getOutputStream());
              dos.writeUTF(request);
              dos.writeUTF(deletename);
    
              serverSocketConnection.close();
            }
    
            clientSocket.close();
            break;  
          case "GET":
          String getFileName = dis.readUTF();

          Socket serverSocketGet = new Socket("localhost", 2025);
          System.out.println("Connected to server on port 2025");
          DataOutputStream dos = new DataOutputStream(serverSocketGet.getOutputStream());
          dos.writeUTF(request);
          dos.writeUTF(getFileName);
          
          DataInputStream inputStreamServer = new DataInputStream(serverSocketGet.getInputStream());
          long fileSizeGet = inputStreamServer.readLong();

          
          byte[] fileContentGet = new byte[(int)fileSizeGet];

          inputStreamServer.readFully(fileContentGet);

          DataOutputStream clientSocketOutput = new DataOutputStream(clientSocket.getOutputStream());
          clientSocketOutput.writeLong(fileSizeGet);
          clientSocketOutput.write(fileContentGet);
           
          serverSocketGet.close();
          clientSocket.close();
          clientSocketOutput.close();
          inputStreamServer.close();
          dos.close();
          dis.close();

          default:
            break;
        }
        
        System.out.println("File transferred to all servers");
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static class ReqStatus extends TimerTask {

    public static int leader = 0;

    // static String[][] fileContents = new String[3][];

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
      ArrayList<Integer> active_ports = new ArrayList<>();
      ArrayList<String> active_hosts = new ArrayList<>();
      ArrayList<String[]> fileContents = new ArrayList<>();
      for (int i = 0; i < SERVER_PORTS.length; i++) {
        // Connect to each server
        try {
          Socket serverSocketConnection =
              new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
          serverSocketConnection.setSoTimeout(2000);
          System.out.println("Connected to server on port " + SERVER_PORTS[i]);

          // Transfer the filename, filesize, and file to the server
          DataOutputStream out =
              new DataOutputStream(serverSocketConnection.getOutputStream());
          DataInputStream in =
              new DataInputStream(serverSocketConnection.getInputStream());
          // Write the file content to the server
          out.writeUTF("STATUS");

          // Receive and print the string array
          int arrayLength = in.readInt();
          String[] documentNames = new String[arrayLength];
          for (int j = 0; j < arrayLength; j++) {
            documentNames[j] = in.readUTF();
          }

          // fileContents[i] = Arrays.copyOf(documentNames,
          // documentNames.length);
          fileContents.add(documentNames);
          // Print the received array
          System.out.println("Documents on server:");
          for (String documentName : documentNames) {
            System.out.println(documentName);
          }

          active_hosts.add(SERVER_HOSTS[i]);
          active_ports.add(SERVER_PORTS[i]);

          serverSocketConnection.close();

        } catch (SocketException s) {
          // isRunning[i] = false;
          // for (int j = 0; j < isRunning.length; j++){
          //     if (isRunning[j]){
          //         leader = j;
          //         break;
          //     }
          // }
          // System.out.println(isRunning[i]);
          // System.out.println("leader: " + leader);
          continue;
        }
      }

      // ArrayList<Integer> active_ports = new ArrayList<>();
      // ArrayList<String> active_hosts = new ArrayList<>();
      // ArrayList<String[]> fileContents = new ArrayList<>();
      System.out.println(
          "----------------------------------------------------");
      for (int i = 0; i < active_ports.size(); i++) {
        System.out.println("Active host: " + active_hosts.get(i) +
                           ", port: " + active_ports.get(i));
        for (int j = 0; j < fileContents.get(i).length; j++) {
          System.out.println(fileContents.get(i)[j]);
        }
      }
      System.out.println(
          "----------------------------------------------------");

      HashMap<Integer, String[][]> differences = new HashMap<>();

      // if (!fileContents[(leader + 1) % 3].equals(fileContents[leader])) {
      //     differences.put((leader + 1) % 3,
      //     compareArrays(fileContents[leader], fileContents[(leader + 1) %
      //     3]));
      // }
      // if (!fileContents[(leader + 2) % 3].equals(fileContents[leader])) {
      //     differences.put((leader + 2) % 3,
      //     compareArrays(fileContents[leader], fileContents[(leader + 2) %
      //     3]));
      // }
      int active_servers = active_ports.size();
      // Assuming the first active server is always the leader for simplicity
      // If the leader might not be the first server, adjust the logic to select
      // the leader based on your criteria
      for (int i = 0; i < active_servers; i++) {
        if (i != leader) { // Skip comparing the leader to itself
          differences.put(
              i, compareArrays(fileContents.get(leader), fileContents.get(i)));
        }
      }
      // Iterate over the HashMap
      for (Map.Entry<Integer, String[][]> entry : differences.entrySet()) {
        Integer key = entry.getKey();
        String[][] value = entry.getValue();

        System.out.println("Key: " + key);
        System.out.println("Values: ");
        for (int i = 0; i < value.length; i++) {
          String type = (i == 0) ? "Plus" : "Minus";
          System.out.println("  " + type + " values:");
          for (String val : value[i]) {
            System.out.println("    " + val);
          }
        }
      }

      for (int i : differences.keySet()) {
        System.out.println("keySet iteration for index: " + i);
        String[][] difArray = differences.get(i);
        System.out.println(": diffArraray[0].length: " + difArray[0].length);
        System.out.println(": diffArraray[1].length: " + difArray[1].length);
        // Check if any file needs to be uploaded to replicas
        if (difArray[0].length != 0) {

        for (int j = 0; j < difArray[0].length; j++) {
          String filename = difArray[0][j];
          // Socket toLeader = new Socket(SERVER_HOSTS[leader],
          // SERVER_PORTS[leader]);
          Socket toLeader = new Socket("localhost", active_ports.get(leader));
          DataInputStream dis = new DataInputStream(toLeader.getInputStream());
          DataOutputStream dos =
              new DataOutputStream(toLeader.getOutputStream());

          dos.writeUTF("GET");
          dos.writeUTF(filename);

          long filesize = dis.readLong();
          byte[] fileContent = new byte[(int)filesize];
          dis.readFully(fileContent);

          toLeader.close();

          // Socket toReplica = new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
          Socket toReplica = new Socket("localhost", active_ports.get(i));
          // DataInputStream replicaInput = new
          // DataInputStream(toReplica.getInputStream());
          DataOutputStream replicaOutput =
              new DataOutputStream(toReplica.getOutputStream());

          replicaOutput.writeUTF("UPLOAD");
          replicaOutput.writeUTF(filename);
          replicaOutput.writeLong(filesize);
          replicaOutput.write(fileContent);

          toReplica.close();
        }
        }
        // Check if any file needs do be deleted from the replicas
         if(difArray[1].length != 0) {
             for (int j = 0; j < difArray[1].length; j++){
                 String filename = difArray[1][j];
                 // Socket deleteSocket = new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
                 Socket deleteSocket = new Socket("localhost", active_ports.get(i));

                        DataOutputStream delOutStream = new
                 DataOutputStream(deleteSocket.getOutputStream());
                 delOutStream.writeUTF("DELETE");
                 delOutStream.writeUTF(filename);

                deleteSocket.close();
            }
        }
      }
    }

    public static String[][] compareArrays(String[] base, String[] compare) {
      HashSet<String> baseSet = new HashSet<>();
      HashSet<String> compareSet = new HashSet<>();
      ArrayList<String> plusValues = new ArrayList<>();
      ArrayList<String> minusValues = new ArrayList<>();

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
      result[0] = plusValues.toArray(new String[0]);  // "+" values
      result[1] = minusValues.toArray(new String[0]); // "-" values

      return result;
    }
  }
}
