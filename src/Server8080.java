import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class Server8080 extends Thread {

    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[256];
    private final ExecutorService executor = Executors.newFixedThreadPool(10); // For handling computation tasks
    private final Object lock = new Object(); // For synchronizing access to the socket

    public Server8080(int port) throws SocketException {
        socket = new DatagramSocket(port);
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

                if (received.startsWith("list")) {
                    handleComputation(address, port);
                } else if (received.startsWith("get ")) {
                    handleComputation(address, port);
                } else if (received.startsWith("compute")) {
                    handleComputation(address, port);
                } else if (received.startsWith("stream ")) {
                    handleComputation( address, port);
                } else {
                    socket.send(new DatagramPacket(buf, buf.length, address, port));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();
        executor.shutdown();
    }


    private void handleComputation(InetAddress address, int port) {
        executor.submit(() -> {
            try {
                synchronized (lock) {
                    // Simulate 150 seconds of computation
                    System.out.println("Computation started...");
                    Thread.sleep(150 * 1000); // 150 seconds
                    System.out.println("Computation completed");
                    String message = "Computation completed";
                    byte[] responseBuf = message.getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length, address, port);
                    socket.send(responsePacket);
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        int port = 8080; // You can change this to any port you want
        try {
            Thread server = new Server8080(port);
            server.start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}