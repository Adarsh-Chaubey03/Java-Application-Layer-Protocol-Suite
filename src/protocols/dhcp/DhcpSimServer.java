package protocols.dhcp;

import core.UdpClient;
import utils.ByteUtils;
import utils.Config;
import utils.Logger;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 * Simulated DHCP server that runs on localhost.
 * Listens for DISCOVER and REQUEST packets, responds with OFFER and ACK.
 * Uses non-standard high ports to avoid privileged port restrictions.
 */
public class DhcpSimServer {

    /** Port this server listens on */
    private final int port;

    /** IP address pool — the next IP to offer */
    private String offeredIp = "192.168.1.100";

    /** Server's own IP */
    private final String serverIp = "192.168.1.1";

    /** Subnet mask */
    private final String subnetMask = "255.255.255.0";

    /** Default gateway */
    private final String router = "192.168.1.1";

    /** DNS server */
    private final String dns = "8.8.8.8";

    /** Lease time in seconds */
    private final int leaseTime = 86400; /* 24 hours */

    /** Flag to control the server loop */
    private volatile boolean running;

    /** The UDP client used for receiving/sending */
    private UdpClient udpClient;

    /**
     * Construct a DHCP simulation server on the specified port.
     *
     * @param port the port to listen on
     */
    public DhcpSimServer(int port) {
        this.port = port;
    }

    /**
     * Start the server and process exactly 2 exchanges (DISCOVER→OFFER, REQUEST→ACK).
     * Runs in a blocking manner.
     */
    public void start() {
        running = true;
        try {
            udpClient = new UdpClient(port, 10000);
            Logger.info("DHCP Simulation Server listening on port " + port);

            int exchangeCount = 0;
            while (running && exchangeCount < 2) {
                try {
                    DatagramPacket incoming = udpClient.receive(576);
                    DhcpPacket request = DhcpPacket.fromBytes(
                            incoming.getData(), incoming.getLength());

                    Logger.info("Received: " + request.summary());

                    DhcpPacket response;
                    if (request.getMessageType() == DhcpPacket.DHCP_DISCOVER) {
                        /* Respond with OFFER */
                        response = DhcpPacket.createOffer(request, offeredIp,
                                serverIp, subnetMask, router, dns, leaseTime);
                    } else if (request.getMessageType() == DhcpPacket.DHCP_REQUEST) {
                        /* Respond with ACK */
                        response = DhcpPacket.createAck(request, offeredIp,
                                serverIp, subnetMask, router, dns, leaseTime);
                    } else {
                        Logger.debug("Ignoring message type: "
                                + DhcpPacket.messageTypeToString(request.getMessageType()));
                        continue;
                    }

                    /* Send response back to the client */
                    byte[] responseBytes = response.toBytes();
                    udpClient.send(responseBytes,
                            incoming.getAddress(), incoming.getPort());
                    Logger.info("Sent: " + response.summary());
                    exchangeCount++;

                } catch (java.net.SocketTimeoutException e) {
                    Logger.debug("Server waiting for DHCP messages...");
                }
            }

        } catch (Exception e) {
            Logger.error("DHCP Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Start the server in a background thread.
     */
    public void startInBackground() {
        Thread serverThread = new Thread(this::start);
        serverThread.setDaemon(true);
        serverThread.start();

        /* Give the server time to bind the port */
        try { Thread.sleep(500); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stop the server gracefully.
     */
    public void stop() {
        running = false;
        if (udpClient != null) {
            udpClient.close();
        }
        Logger.info("DHCP Simulation Server stopped");
    }
}
