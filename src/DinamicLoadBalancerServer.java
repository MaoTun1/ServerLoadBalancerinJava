import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


class Server {
    private String ip;
    private int port;

    public Server(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Server{" +
                "ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}

public class DinamicLoadBalancerServer extends Thread {
    private Map<Server, Integer> serverConnections;
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];
    private boolean isDinamic;
   

    public DinamicLoadBalancerServer(int port, boolean isDinamic) throws SocketException {
        this.serverConnections = new HashMap<>();
        this.isDinamic=isDinamic;
    }

    public void addServer(Server server) {
        serverConnections.put(server, 0);
    }

    private Server getNextServer() {
            return getNextServerDinamic();
    }

    private Server getNextServerDinamic() {
        int minConnections = Integer.MAX_VALUE;
        Server selectedServer = null;
 
        for (Entry<Server, Integer> entry : serverConnections.entrySet()) { 
            if (entry.getValue() <= minConnections) {
                minConnections = entry.getValue();
                selectedServer = entry.getKey();
            }
        }
 
        // Increment the connection count for the selected server
        if (selectedServer != null) {
            serverConnections.put(selectedServer, minConnections + 1);
        }
 
        return selectedServer;
    }
    

   

    public void run() {
        running = true;

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                String received = new String(packet.getData(), 0, packet.getLength());

                if (received.startsWith("end")) {
                    running = false;
                    continue;
                }

                Server targetServer = getNextServer();
                System.out.println("Redirecting request to: " + targetServer);
                redirectRequest(packet, targetServer);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }

    private void redirectRequest(DatagramPacket packet, Server targetServer) throws IOException {
        DatagramSocket forwardSocket = new DatagramSocket();
        DatagramPacket forwardPacket = new DatagramPacket(
                packet.getData(),
                packet.getLength(),
                InetAddress.getByName(targetServer.getIp()),
                targetServer.getPort()
        );

        forwardSocket.send(forwardPacket);
        forwardSocket.close();
    }

    public static void main(String[] args) throws SocketException {
        
        try {
            DinamicLoadBalancerServer loadBalancerServer = new DinamicLoadBalancerServer(4445, true); // true for static, false for dynamic
            loadBalancerServer.addServer(new Server("localhost", 8080));
            loadBalancerServer.addServer(new Server("localhost", 8081));
            loadBalancerServer.addServer(new Server("localhost", 8082));
            
            loadBalancerServer.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        
    }
}