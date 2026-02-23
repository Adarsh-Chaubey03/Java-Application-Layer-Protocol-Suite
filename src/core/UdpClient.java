package core;

import java.io.IOException;
import java.net.*;
import utils.Logger;

/**
 * Reusable UDP datagram client wrapper.
 * Provides send and receive operations for datagram-based protocols (DNS, DHCP).
 * Includes configurable timeout and buffer size handling.
 */
public class UdpClient implements AutoCloseable {

    /** Underlying UDP socket */
    private DatagramSocket socket;

    /** Receive timeout in milliseconds */
    private final int timeout;

    /**
     * Construct a UdpClient with the specified timeout.
     *
     * @param timeout receive timeout in milliseconds
     * @throws IOException if socket creation fails
     */
    public UdpClient(int timeout) throws IOException {
        this.timeout = timeout;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(timeout);
        Logger.debug("UDP socket created (timeout=" + timeout + "ms)");
    }

    /**
     * Construct a UdpClient bound to a specific port (used by servers).
     *
     * @param port    the local port to bind to
     * @param timeout receive timeout in milliseconds
     * @throws IOException if socket creation or binding fails
     */
    public UdpClient(int port, int timeout) throws IOException {
        this.timeout = timeout;
        this.socket = new DatagramSocket(port);
        this.socket.setSoTimeout(timeout);
        Logger.debug("UDP socket bound to port " + port + " (timeout=" + timeout + "ms)");
    }

    /**
     * Send a byte array to the specified host and port.
     *
     * @param data the byte array to send
     * @param host the destination hostname or IP
     * @param port the destination port
     * @throws IOException if sending fails
     */
    public void send(byte[] data, String host, int port) throws IOException {
        InetAddress address = InetAddress.getByName(host);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
        Logger.debug("Sent " + data.length + " bytes to " + host + ":" + port);
    }

    /**
     * Send a byte array to the specified InetAddress and port.
     *
     * @param data    the byte array to send
     * @param address the destination InetAddress
     * @param port    the destination port
     * @throws IOException if sending fails
     */
    public void send(byte[] data, InetAddress address, int port) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
        Logger.debug("Sent " + data.length + " bytes to " + address + ":" + port);
    }

    /**
     * Receive a datagram packet with the specified buffer size.
     *
     * @param bufferSize the maximum buffer size for receiving
     * @return the received DatagramPacket
     * @throws IOException if receiving fails or times out
     */
    public DatagramPacket receive(int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        Logger.debug("Received " + packet.getLength() + " bytes from "
                + packet.getAddress() + ":" + packet.getPort());
        return packet;
    }

    /**
     * Get the underlying DatagramSocket.
     *
     * @return the DatagramSocket instance
     */
    public DatagramSocket getSocket() {
        return socket;
    }

    /**
     * Close the UDP socket and release resources.
     */
    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            Logger.debug("UDP socket closed");
        }
    }
}
