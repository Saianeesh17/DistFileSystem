import java.io.*;
import java.net.*;

public class ServerTwo{

    int port = 2026;
    Socket server;
    ServerSocket serverSocket;
    PrintWriter out;
    BufferedReader in;
    
    public ServerTwo(){
        
        
    }

    public void startServer() {
        try {
            serverSocket = new ServerSocket(this.port);
            System.out.println("Server listening on port" + port);
            server = serverSocket.accept();
            out = new PrintWriter(server.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(server.getInputStream()));
            String greeting = in.readLine();
            if ("hello server".equals(greeting)) {
                out.println("hello client");
            }
            else {
                out.println("unrecognised greeting");
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args){
        // System.out.println("Hello World");
        ServerOne serverOne =  new ServerOne();
        serverOne.startServer();
    }
}
