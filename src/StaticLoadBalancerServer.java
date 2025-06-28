import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

public class StaticLoadBalancerServer extends Thread {
    private DatagramSocket socket;
    private List<Server> servers;
    private boolean running;
    private byte[] buf = new byte[256];
    private boolean isStatic;
    private AtomicInteger currentIndex = new AtomicInteger(0);

    public StaticLoadBalancerServer(int port, boolean isStatic) throws SocketException {
        socket = new DatagramSocket(port);
        this.servers = new ArrayList<>();
        this.isStatic = isStatic;
    }

    public void addServer(Server server) {
        servers.add(server);
    }

    private Server getNextServer() {
            return getNextServerStatic();
    }

    private Server getNextServerStatic() {
        int index = currentIndex.getAndIncrement() % servers.size();
        return servers.get(index);
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

    public static void main(String[] args) {
        try {
            StaticLoadBalancerServer loadBalancerServer = new StaticLoadBalancerServer(4445, true); // true for static, false for dynamic
            loadBalancerServer.addServer(new Server("localhost", 8080));
            loadBalancerServer.addServer(new Server("localhost", 8081));
            loadBalancerServer.addServer(new Server("localhost", 8082));
            
            loadBalancerServer.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}