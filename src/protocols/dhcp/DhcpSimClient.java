package protocols.dhcp;

import core.UdpClient;
import utils.ByteUtils;
import utils.Config;
import utils.Logger;

import java.net.DatagramPacket;

/**
 * Simulated DHCP client that performs the full DORA sequence:
 *   DISCOVER → (receive OFFER) → REQUEST → (receive ACK)
 *
 * Communicates with DhcpSimServer on localhost using non-standard ports.
 */
public class DhcpSimClient {

    /** Server port to send to */
    private final int serverPort;

    /** Simulated MAC address */
    private final byte[] macAddress;

    /**
     * Construct a DHCP simulation client.
     *
     * @param serverPort the port the simulated server listens on
     */
    public DhcpSimClient(int serverPort) {
        this.serverPort = serverPort;
        this.macAddress = new byte[]{
                (byte) 0xAA, (byte) 0xBB, (byte) 0xCC,
                (byte) 0xDD, (byte) 0xEE, (byte) 0xFF
        };
    }

    /**
     * Run the full DHCP DORA exchange.
     * Sends DISCOVER, waits for OFFER, sends REQUEST, waits for ACK.
     */
    public void runDora() {
        Logger.section("DHCP DORA Sequence (Simulated)");

        try (UdpClient udpClient = new UdpClient(Config.getDefaultReadTimeout())) {

            /* ── Step 1: DISCOVER ── */
            Logger.info("Step 1: Sending DHCP DISCOVER");
            DhcpPacket discover = DhcpPacket.createDiscover(macAddress);
            byte[] discoverBytes = discover.toBytes();
            udpClient.send(discoverBytes, "127.0.0.1", serverPort);
            System.out.println("  → DISCOVER sent (broadcast, looking for any DHCP server)");

            /* ── Step 2: Receive OFFER ── */
            Logger.info("Step 2: Waiting for DHCP OFFER");
            DatagramPacket offerPacket = udpClient.receive(576);
            DhcpPacket offer = DhcpPacket.fromBytes(offerPacket.getData(), offerPacket.getLength());
            String offeredIp = ByteUtils.bytesToIp(offer.getYiaddr(), 0);
            String serverIp = ByteUtils.bytesToIp(offer.getSiaddr(), 0);
            System.out.println("  ← OFFER received: IP=" + offeredIp + " from server=" + serverIp);
            System.out.println("    Subnet Mask : " + ByteUtils.bytesToIp(offer.getSubnetMask(), 0));
            System.out.println("    Gateway     : " + ByteUtils.bytesToIp(offer.getRouter(), 0));
            System.out.println("    DNS Server  : " + ByteUtils.bytesToIp(offer.getDnsServer(), 0));
            System.out.println("    Lease Time  : " + offer.getLeaseTime() + " seconds");

            /* ── Step 3: REQUEST ── */
            Logger.info("Step 3: Sending DHCP REQUEST");
            DhcpPacket request = DhcpPacket.createRequest(discover, offeredIp, serverIp);
            byte[] requestBytes = request.toBytes();
            udpClient.send(requestBytes, "127.0.0.1", serverPort);
            System.out.println("  → REQUEST sent (requesting IP=" + offeredIp + ")");

            /* ── Step 4: Receive ACK ── */
            Logger.info("Step 4: Waiting for DHCP ACK");
            DatagramPacket ackPacket = udpClient.receive(576);
            DhcpPacket ack = DhcpPacket.fromBytes(ackPacket.getData(), ackPacket.getLength());
            String assignedIp = ByteUtils.bytesToIp(ack.getYiaddr(), 0);
            System.out.println("  ← ACK received: IP=" + assignedIp + " confirmed!");
            System.out.println("    Lease Time  : " + ack.getLeaseTime() + " seconds");

            Logger.separator();
            System.out.println("  DHCP DORA sequence completed successfully!");
            System.out.println("  Assigned IP: " + assignedIp);
            System.out.println("  MAC Address: " + formatMac(macAddress));

        } catch (Exception e) {
            Logger.error("DHCP client error: " + e.getMessage());
        }
    }

    /**
     * Format a MAC address as a human-readable string.
     *
     * @param mac the 6-byte MAC address
     * @return formatted MAC string (e.g., "AA:BB:CC:DD:EE:FF")
     */
    private String formatMac(byte[] mac) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            if (i > 0) sb.append(":");
            sb.append(String.format("%02X", mac[i] & 0xFF));
        }
        return sb.toString();
    }
}
