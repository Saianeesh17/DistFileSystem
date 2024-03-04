import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String resp;

    public void startConnection(String ip, int port) {
        
        try{
            clientSocket = new Socket(ip, port);
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String sendMessage(String msg) {
        try{
            out.println(msg);
            resp = in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resp;
        
    }

    public void stopConnection() {

        try{
            out.close();
            clientSocket.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        Client client = new Client();
        client.startConnection("127.0.0.1", 2027);
        String response = client.sendMessage("client3");
        System.out.println(response);
    }
}
