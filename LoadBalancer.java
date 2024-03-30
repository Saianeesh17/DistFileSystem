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
  public static ArrayList<Integer> ACTIVE_SERVER_PORTS = new ArrayList<>();
  public static ArrayList<String> ACTIVE_SERVER_HOSTS = new ArrayList<>();
  public static Map<Integer, Boolean> serverStatus = new HashMap<>();

  public static void removeServer(int port_number) {
    for (int i = 0; i < ACTIVE_SERVER_PORTS.size(); i++) {
      if (port_number == ACTIVE_SERVER_PORTS.get(i)) {
        ACTIVE_SERVER_PORTS.remove(i);
        ACTIVE_SERVER_HOSTS.remove(i);
      }
    }
  }

  static {
    for (int i = 0; i < SERVER_PORTS.length; i++) {
      // Populate HashMap with ports
      serverStatus.put(SERVER_PORTS[i], false);
    }
  }

  // public static boolean[] isRunning = {true, true, true};

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

          // for (int i = 0; i < SERVER_PORTS.length; i++) {
          for (int i = 0; i < ACTIVE_SERVER_PORTS.size(); i++) {
            try {

              Socket serverSocketConnection =
                  // new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
                  new Socket(ACTIVE_SERVER_HOSTS.get(i),
                             ACTIVE_SERVER_PORTS.get(i));
              serverSocketConnection.setSoTimeout(2000);
              System.out.println("Connected to server on port " +
                                 // SERVER_PORTS[i]);
                                 ACTIVE_SERVER_PORTS.get(i));
              DataOutputStream dos = new DataOutputStream(
                  serverSocketConnection.getOutputStream());
              dos.writeUTF(request);
              dos.writeUTF(filename);
              dos.writeLong(filesize);
              dos.write(fileContent);

              serverSocketConnection.close();
            } catch (SocketException e) {
              serverStatus.put(ACTIVE_SERVER_PORTS.get(i), false);
              removeServer(ACTIVE_SERVER_PORTS.get(i));
            }
            // Need to change. If serverone is down it will not send
          }

          clientSocket.close();
          break;

        case "DELETE":
          String deletename = dis.readUTF();
          // for (int i = 0; i < SERVER_PORTS.length; i++) {
          for (int i = 0; i < ACTIVE_SERVER_PORTS.size(); i++) {
            // Need to change. If serverone is down it will not delete
            Socket serverSocketConnection =
                // new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
                new Socket(ACTIVE_SERVER_HOSTS.get(i),
                           ACTIVE_SERVER_PORTS.get(i));
            System.out.println("Connected to server on port " +
                               SERVER_PORTS[i]);

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
          // Need to change
          Socket serverSocketGet = new Socket("localhost", 2025);
          System.out.println("Connected to server on port 2025");
          DataOutputStream dos =
              new DataOutputStream(serverSocketGet.getOutputStream());
          dos.writeUTF(request);
          dos.writeUTF(getFileName);

          DataInputStream inputStreamServer =
              new DataInputStream(serverSocketGet.getInputStream());
          long fileSizeGet = inputStreamServer.readLong();

          byte[] fileContentGet = new byte[(int)fileSizeGet];

          inputStreamServer.readFully(fileContentGet);

          DataOutputStream clientSocketOutput =
              new DataOutputStream(clientSocket.getOutputStream());
          clientSocketOutput.writeLong(fileSizeGet);
          clientSocketOutput.write(fileContentGet);

          serverSocketGet.close();
          clientSocket.close();
          clientSocketOutput.close();
          inputStreamServer.close();
          dos.close();
          dis.close();
          break;

        default:
          break;
        }

        System.out.println("File transferred to all servers");
      } catch (IOException e) {
        // } catch (SocketException e) {
        // e.printStackTrace();
        // Remove Active server from active servers
        // serverStatus.put(ACTIVE_SERVER_PORTS.get(i), false);
        // ACTIVE_SERVER_PORTS.remove(i);
        // ACTIVE_SERVER_HOSTS.remove(i);
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
        // TODO: Update the Active ports list
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public static void checkServerStatus() throws IOException {
      ArrayList<Integer> active_ports = new ArrayList<>();
      ArrayList<String> active_hosts = new ArrayList<>();
      ArrayList<String[]> fileContents = new ArrayList<>();
      HashMap<Integer, String[]> fileContentss = new HashMap();
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
          System.out.println("## Made it here! ##");

          // if the ports previous state was inactive:
          // set the port to active
          // add the port to the end of the active_ports list
          if (serverStatus.get(SERVER_PORTS[i]) == false) {
            ACTIVE_SERVER_PORTS.add(SERVER_PORTS[i]);
            ACTIVE_SERVER_HOSTS.add(SERVER_HOSTS[i]);
            serverStatus.put(SERVER_PORTS[i], true);
          }

          // fileContents[i] = Arrays.copyOf(documentNames,
          // documentNames.length);

          fileContents.add(documentNames);
          fileContentss.put(SERVER_PORTS[i], documentNames);

          // Print the received array
          System.out.print("Documents on server: ");
          for (String documentName : documentNames) {
            System.out.print(documentName + " ");
          }
          System.out.println();

          active_hosts.add(SERVER_HOSTS[i]);
          active_ports.add(SERVER_PORTS[i]);

          serverSocketConnection.close();

        } catch (SocketException s) {
          // if (serverStatus.get(SERVER_PORTS[i]) == true){
          // }
          // Change the server state to false
          if (serverStatus.get(SERVER_PORTS[i])) {
            removeServer(SERVER_PORTS[i]);
            serverStatus.put(SERVER_PORTS[i], false);
          }
          // Remove the server from the active server host/port list
          continue;
        }
      }

      // Iterate through final list and remove ports that are inactive
      // from the list
      if (ACTIVE_SERVER_PORTS.size() == 0) {
        System.out.println("No active servers.");
        return;
      }

      int leader_port = ACTIVE_SERVER_PORTS.get(0);
      System.out.print("ACTIVE_SERVER_PORTS: ");
      for (Integer port : ACTIVE_SERVER_PORTS) {
        System.out.print(port + " ");
      }
      System.out.println("\nleader_port: " + leader_port);

      System.out.println(
          "----------------------------------------------------");
      // for (int i = 0; i < active_ports.size(); i++) {
      for (int i = 0; i < ACTIVE_SERVER_PORTS.size(); i++) {
        // System.out.println("Active host: " + active_hosts.get(i) +
        System.out.println("Active host: " + ACTIVE_SERVER_HOSTS.get(i) +
                           // ", port: " + active_ports.get(i));
                           ", port: " + ACTIVE_SERVER_PORTS.get(i));
        // for (int j = 0; j < fileContents.get(i).length; j++) {
        int fileContents_len =
            fileContentss.get(ACTIVE_SERVER_PORTS.get(i)).length;
        if (fileContents_len > 0) {
          for (int j = 0;
               j < fileContentss.get(ACTIVE_SERVER_PORTS.get(i)).length; j++) {
            // System.out.println(fileContents.get(i)[j]);
            System.out.println(
                fileContentss.get(ACTIVE_SERVER_PORTS.get(i))[j]);
          }
        }
      }
      System.out.println(
          "----------------------------------------------------");

      HashMap<Integer, String[][]> differences = new HashMap<>();
      // int active_servers = active_ports.size();
      int active_servers = ACTIVE_SERVER_PORTS.size();
      // Assuming the first active server is always the leader for simplicity
      // If the leader might not be the first server, adjust the logic to select
      // the leader based on your criteria
      for (int i = 1; i < active_servers; i++) {
        // if (i != leader) { // Skip comparing the leader to itself
        // differences.put(
        //     i, compareArrays(fileContents.get(leader), fileContents.get(i)));
        differences.put(
            i, compareArrays(fileContentss.get(leader_port),
                             fileContentss.get(ACTIVE_SERVER_PORTS.get(i))));
        // }
      }
      // Iterate over the HashMap
      // for (Map.Entry<Integer, String[][]> entry : differences.entrySet()) {
      //   Integer key = entry.getKey();
      //   String[][] value = entry.getValue();

      //   System.out.println("Key: " + key);
      //   System.out.println("Values: ");
      //   for (int i = 0; i < value.length; i++) {
      //     String type = (i == 0) ? "Plus" : "Minus";
      //     System.out.println("  " + type + " values:");
      //     for (String val : value[i]) {
      //       System.out.println("    " + val);
      //     }
      //   }
      // }
      for (int i : differences.keySet()) {
        // System.out.println("keySet iteration for index: " + i);
        String[][] difArray = differences.get(i);
        // System.out.println(": diffArraray[0].length: " + difArray[0].length);
        // System.out.println(": diffArraray[1].length: " + difArray[1].length);
        // Check if any file needs to be uploaded to replicas
        if (difArray[0].length != 0) {

          for (int j = 0; j < difArray[0].length; j++) {
            String filename = difArray[0][j];
            // Socket toLeader = new Socket(SERVER_HOSTS[leader],
            // SERVER_PORTS[leader]);
            // Socket toLeader = new Socket("localhost",
            // active_ports.get(leader));
            Socket toLeader = new Socket(ACTIVE_SERVER_HOSTS.get(leader),
                                         ACTIVE_SERVER_PORTS.get(leader));
            DataInputStream dis =
                new DataInputStream(toLeader.getInputStream());
            DataOutputStream dos =
                new DataOutputStream(toLeader.getOutputStream());

            dos.writeUTF("GET");
            dos.writeUTF(filename);

            long filesize = dis.readLong();
            byte[] fileContent = new byte[(int)filesize];
            dis.readFully(fileContent);

            toLeader.close();

            // Socket toReplica = new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
            // Socket toReplica = new Socket("localhost", active_ports.get(i));
            Socket toReplica =
                new Socket("localhost", ACTIVE_SERVER_PORTS.get(i));
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
        if (difArray[1].length != 0) {
          for (int j = 0; j < difArray[1].length; j++) {
            String filename = difArray[1][j];
            // Socket deleteSocket = new Socket(SERVER_HOSTS[i],
            // SERVER_PORTS[i]);
            // Socket deleteSocket = new Socket("localhost",
            // active_ports.get(i));
            Socket deleteSocket =
                new Socket("localhost", ACTIVE_SERVER_PORTS.get(i));

            DataOutputStream delOutStream =
                new DataOutputStream(deleteSocket.getOutputStream());
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
