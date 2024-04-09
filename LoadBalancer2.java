import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class LoadBalancer2 {
  // ip and port info of all servers, along with lists of active servers
  public static final int PORT = 2029;
  public static final int[] SERVER_PORTS = {2025, 2026, 2028};
  public static final String[] SERVER_HOSTS = {"10.13.92.30", "10.13.92.30",
                                               "10.13.92.30"}; 
  public static ArrayList<Integer> ACTIVE_SERVER_PORTS = new ArrayList<>();
  public static ArrayList<String> ACTIVE_SERVER_HOSTS = new ArrayList<>();
  public static Map<Integer, Boolean> serverStatus = new HashMap<>();

  // removing server from active server hosts and ports
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

  public static void main(String[] args) {
    // Start the server logic in a new thread
    new Thread(new ServerLogic()).start();
    // Timer thread to check status of server every 5 seconds
    Timer timer = new Timer();
    ReqStatus req = new ReqStatus();
    timer.schedule(req, 0, 5000);
    // Other operations can be performed here concurrently
  }

  // accept incoming requests from clients
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

  // Thread to process requests from the client
  public static class ProcessReq implements Runnable {
    private Socket clientSocket;

    public ProcessReq(Socket clientSocket) { this.clientSocket = clientSocket; }

    @Override
    public void run() {
      try {
        DataInputStream dis =
            new DataInputStream(clientSocket.getInputStream());
        String request = dis.readUTF();
        // switch-case to handle different request types
        switch (request) {
        case "UPLOAD":
          // read file content from clients
          String filename = dis.readUTF();
          long filesize = dis.readLong();

          byte[] fileContent = new byte[(int)filesize];
          dis.readFully(fileContent);
          // connect and send file content to all active servers
          for (int i = 0; i < ACTIVE_SERVER_PORTS.size(); i++) {
            try {

              Socket serverSocketConnection =
                  new Socket(ACTIVE_SERVER_HOSTS.get(i),
                             ACTIVE_SERVER_PORTS.get(i));
              serverSocketConnection.setSoTimeout(2000);
              System.out.println("Connected to server on port " +
                                 ACTIVE_SERVER_PORTS.get(i));
              DataOutputStream dos = new DataOutputStream(
                  serverSocketConnection.getOutputStream());
              dos.writeUTF(request);
              dos.writeUTF(filename);
              dos.writeLong(filesize);
              dos.write(fileContent);

              serverSocketConnection.close();
            
            // handle server crashes
            } catch (SocketException e) {
              serverStatus.put(ACTIVE_SERVER_PORTS.get(i), false);
              removeServer(ACTIVE_SERVER_PORTS.get(i));
              i--; // Adjust loop index
            }
          }
          System.out.println("File transferred to all servers");
          clientSocket.close();
          break;

        // delete file from all active servers
        case "DELETE":
          String deletename = dis.readUTF();
          for (int i = 0; i < ACTIVE_SERVER_PORTS.size(); i++) {
            try {
              Socket serverSocketConnection =
                  new Socket(ACTIVE_SERVER_HOSTS.get(i),
                             ACTIVE_SERVER_PORTS.get(i));
              serverSocketConnection.setSoTimeout(2000);
              System.out.println("Connected to server on port " +
                                 ACTIVE_SERVER_PORTS.get(i));

              DataOutputStream dos = new DataOutputStream(
                  serverSocketConnection.getOutputStream());
              dos.writeUTF(request);
              dos.writeUTF(deletename);

              serverSocketConnection.close();
            } catch (SocketException e) {
              serverStatus.put(ACTIVE_SERVER_PORTS.get(i), false);
              removeServer(ACTIVE_SERVER_PORTS.get(i));
              i--; // Adjust loop index
            }
          }

          clientSocket.close();
          break;

        // get file from first available server
        case "GET":
          String getFileName = dis.readUTF();
          boolean fileSent = false;

          for (int i = 0; i < ACTIVE_SERVER_PORTS.size(); i++) {
            try {
              Socket serverSocketGet = new Socket(ACTIVE_SERVER_HOSTS.get(i),
                                                  ACTIVE_SERVER_PORTS.get(i));
              serverSocketGet.setSoTimeout(2000);
              System.out.println("Connected to server on port " +
                                 ACTIVE_SERVER_PORTS.get(i));

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

              fileSent = true; // Mark as file sent successfully

              // Close resources for this server connection
              serverSocketGet.close();
              dos.close();
              inputStreamServer.close();

              if (fileSent) {
                break; // Exit the loop if file sent successfully
              }

            } catch (SocketException e) {
              System.out.println("Server on port " +
                                 ACTIVE_SERVER_PORTS.get(i) +
                                 " is not available.");
              serverStatus.put(ACTIVE_SERVER_PORTS.get(i),
                               false); // Mark server as down
              removeServer(
                  ACTIVE_SERVER_PORTS.get(i)); // Remove server from active list
              i--;                             // Adjust loop index
                                               // iterating over
            }
          }

          // If loop exits and file hasn't been sent, it means all servers were
          // tried and failed
          if (!fileSent) {
            System.out.println(
                "Failed to send file. All servers are down or unreachable.");
            // Optionally, send an error message back to the client indicating
            // failure
          }

          // Closing the client socket should happen outside the loop, after all
          // attempts
          dis.close();
          clientSocket.close();
          break;

        default:
          break;
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  // thread to check status of all servers in program
  public static class ReqStatus extends TimerTask {
    // index of the leader
    public static int leader = 0;

    @Override
    public void run() {
      try {
        checkServerStatus();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public static void checkServerStatus() throws IOException {
      ArrayList<Integer> active_ports = new ArrayList<>();
      ArrayList<String> active_hosts = new ArrayList<>();
      HashMap<Integer, String[]> fileContents = new HashMap();
      for (int i = 0; i < SERVER_PORTS.length; i++) {
        // Connect to each server
        try {
          Socket serverSocketConnection =
              new Socket(SERVER_HOSTS[i], SERVER_PORTS[i]);
          serverSocketConnection.setSoTimeout(2000);
          System.out.println("Connected to server on port " + SERVER_PORTS[i]);

          DataOutputStream out =
              new DataOutputStream(serverSocketConnection.getOutputStream());
          DataInputStream in =
              new DataInputStream(serverSocketConnection.getInputStream());
          // Write the request content to the server
          out.writeUTF("STATUS");
          // read the status response from the server which contatins the db content of all the servers
          int arrayLength = in.readInt();
          String[] documentNames = new String[arrayLength];
          for (int j = 0; j < arrayLength; j++) {
            documentNames[j] = in.readUTF();
          }

          // if the ports previous state was inactive:
          // set the port to active
          // add the port to the end of the active_ports list
          if (serverStatus.get(SERVER_PORTS[i]) == false) {
            ACTIVE_SERVER_PORTS.add(SERVER_PORTS[i]);
            ACTIVE_SERVER_HOSTS.add(SERVER_HOSTS[i]);
            serverStatus.put(SERVER_PORTS[i], true);
          }

          fileContents.put(SERVER_PORTS[i], documentNames);

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
      for (int i = 0; i < ACTIVE_SERVER_PORTS.size(); i++) {
        System.out.println("Active host: " + ACTIVE_SERVER_HOSTS.get(i) +
                           ", port: " + ACTIVE_SERVER_PORTS.get(i));
        int fileContents_len =
            fileContents.get(ACTIVE_SERVER_PORTS.get(i)).length;
        if (fileContents_len > 0) {
                    System.out.print("file_contents: ");
          for (int j = 0;
               j < fileContents.get(ACTIVE_SERVER_PORTS.get(i)).length; j++) {
            System.out.print(
                fileContents.get(ACTIVE_SERVER_PORTS.get(i))[j] + " ");
          }
          System.out.println();
        }
      }
      System.out.println(
          "----------------------------------------------------");

      HashMap<Integer, String[][]> differences = new HashMap<>();
      int active_servers = ACTIVE_SERVER_PORTS.size();
      // Assuming the first active server is always the leader for simplicity
      // If the leader might not be the first server, adjust the logic to select
      // the leader based on your criteria
      for (int i = 1; i < active_servers; i++) {
        differences.put(
            i, compareArrays(fileContents.get(leader_port),
                             fileContents.get(ACTIVE_SERVER_PORTS.get(i))));
        
      }

      for (int i : differences.keySet()) {
        String[][] difArray = differences.get(i);
        // Check if any file needs to be uploaded to replicas
        if (difArray[0].length != 0) {

          for (int j = 0; j < difArray[0].length; j++) {
            String filename = difArray[0][j];
            Socket toLeader = new Socket(ACTIVE_SERVER_HOSTS.get(leader),
                                         ACTIVE_SERVER_PORTS.get(leader));
            DataInputStream dis =
                new DataInputStream(toLeader.getInputStream());
            DataOutputStream dos =
                new DataOutputStream(toLeader.getOutputStream());
            // get file from leader server
            dos.writeUTF("GET");
            dos.writeUTF(filename);

            long filesize = dis.readLong();
            byte[] fileContent = new byte[(int)filesize];
            dis.readFully(fileContent);

            toLeader.close();

            Socket toReplica =
                new Socket(ACTIVE_SERVER_HOSTS.get(i), ACTIVE_SERVER_PORTS.get(i));

            DataOutputStream replicaOutput =
                new DataOutputStream(toReplica.getOutputStream());
            // replicate missing files on replicas
            replicaOutput.writeUTF("UPLOAD");
            replicaOutput.writeUTF(filename);
            replicaOutput.writeLong(filesize);
            replicaOutput.write(fileContent);

            toReplica.close();
          }
        }
        // Check if any file needs do be deleted from the replicas
        if (difArray[1].length != 0) {
          // delete extra files on replicas
          for (int j = 0; j < difArray[1].length; j++) {
            String filename = difArray[1][j];
            Socket deleteSocket =
                new Socket(ACTIVE_SERVER_HOSTS.get(i), ACTIVE_SERVER_PORTS.get(i));

            DataOutputStream delOutStream =
                new DataOutputStream(deleteSocket.getOutputStream());
            delOutStream.writeUTF("DELETE");
            delOutStream.writeUTF(filename);

            deleteSocket.close();
          }
        }
      }
    }
    // compare replica server dbs to leader server db
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
      result[0] = plusValues.toArray(new String[0]);  // "+" (add file) values
      result[1] = minusValues.toArray(new String[0]); // "-" (remove file) values

      return result;
    }
  }
}
