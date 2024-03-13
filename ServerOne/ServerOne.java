package ServerOne;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerOne{

    int port = 2025;
    Socket server;
    ServerSocket serverSocket;
    PrintWriter out;
    BufferedReader in;
    static String saveDirectory = "./received_files/";
    private static DataOutputStream dos = null;
    private static DataInputStream dis = null;
    
    public ServerOne(){
        
        
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(this.port);
            System.out.println("Server listening on port" + port);
            while(true){
                server = serverSocket.accept();
                dis = new DataInputStream(server.getInputStream());
                dos = new DataOutputStream(server.getOutputStream());
                
                String request = dis.readUTF();

                switch(request){
                    case "UPLOAD":
                        receiveFile(saveDirectory + dis.readUTF());
                        System.out.println("File successfully received !");
                    break;
                    case "STATUS":
                        System.out.println("Checking server's status");
                        statusCheck(server);
                    break;
                    case "DELETE":
                        deleteFile(saveDirectory + dis.readUTF());
                    break;
                    case "GET":

                    break;
                    default:
                    System.out.println("Wrong request!\n");

                }
                
                //dis.close();
                //dos.close();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteFile(String filePath) throws Exception{
        File fileToDelete = new File(filePath);
        if(fileToDelete.exists()){
            fileToDelete.delete();
        }
    }

    private static void statusCheck(Socket clientSocket) throws IOException{
        // Get files name and put them into an array
        String[] filesArray = getFilesArray();
        Arrays.sort(filesArray);
        //Sending the sorted array back
        dos.writeInt(filesArray.length);
        for (String name : filesArray) {
            dos.writeUTF(name);
        }

    }

    private static String[] getFilesArray() {
        File folder = new File(saveDirectory);
        File[] listOfFiles = folder.listFiles();
        String[] fileArray = new String[0];

        if (listOfFiles != null) {
            fileArray = Arrays.stream(listOfFiles)
                    .filter(File::isFile)
                    .map(File::getName)
                    .toArray(String[]::new);
        }

        return fileArray;
    }
    
    private static void receiveFile(String filePath) throws Exception{
        FileOutputStream fos = new FileOutputStream(filePath);
        long fileSize = dis.readLong();
        byte[] buffer = new byte[4096];

        int bytesRead;
        while (fileSize > 0 && (bytesRead = dis.read(buffer, 0, (int)Math.min(buffer.length, fileSize))) != -1) {
            fos.write(buffer, 0, bytesRead);
            fileSize -= bytesRead;
        }
        fos.close();
    }

    public static void main(String[] args){
        // System.out.println("Hello World");
        ServerOne serverOne =  new ServerOne();
        serverOne.startServer();
    }
}