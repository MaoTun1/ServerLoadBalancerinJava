import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class Client {

    private DatagramSocket socket;
    private InetAddress address;
    private byte[] buf;

    public Client() throws UnknownHostException, SocketException {
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");
    }

    public String sendRequest(String request) throws IOException {
        buf = request.getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445); // 4445 is the LoadBalancerServer port
        socket.send(packet);

        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        String received = new String(packet.getData(), 0, packet.getLength());
        return received;
    }

    public void close() {
        socket.close();
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("Enter request (list, get 'filename', compute 'seconds', stream 'seconds', or end):");
                String request = scanner.nextLine();

                if (request.equals("end")) {
                    client.sendRequest("end");
                    break;
                }

                String response = client.sendRequest(request);
                System.out.println("Response: " + response);
            }

            client.close();
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}