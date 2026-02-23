package protocols.dhcp;

import utils.ByteUtils;
import utils.Logger;

import java.util.Random;

/**
 * DHCP packet builder and decoder per RFC 2131 / RFC 2132.
 *
 * DHCP packet structure (simplified):
 *   op(1) | htype(1) | hlen(1) | hops(1) |
 *   xid(4) |
 *   secs(2) | flags(2) |
 *   ciaddr(4) | yiaddr(4) | siaddr(4) | giaddr(4) |
 *   chaddr(16) |
 *   sname(64) | file(128) |
 *   magic cookie(4) | options(variable)
 *
 * Minimum size: 240 bytes + options
 */
public class DhcpPacket {

    /* ── Operation codes ── */
    public static final byte OP_REQUEST = 1;  /* Client → Server */
    public static final byte OP_REPLY   = 2;  /* Server → Client */

    /* ── Hardware type ── */
    public static final byte HTYPE_ETHERNET = 1;
    public static final byte HLEN_ETHERNET  = 6;

    /* ── DHCP message types (option 53) ── */
    public static final byte DHCP_DISCOVER = 1;
    public static final byte DHCP_OFFER    = 2;
    public static final byte DHCP_REQUEST  = 3;
    public static final byte DHCP_DECLINE  = 4;
    public static final byte DHCP_ACK      = 5;
    public static final byte DHCP_NAK      = 6;
    public static final byte DHCP_RELEASE  = 7;

    /* ── DHCP option codes ── */
    public static final byte OPT_SUBNET_MASK    = 1;
    public static final byte OPT_ROUTER         = 3;
    public static final byte OPT_DNS_SERVER     = 6;
    public static final byte OPT_REQUESTED_IP   = 50;
    public static final byte OPT_LEASE_TIME     = 51;
    public static final byte OPT_MSG_TYPE       = 53;
    public static final byte OPT_SERVER_ID      = 54;
    public static final byte OPT_END            = (byte) 255;

    /* ── Magic cookie (RFC 2131) ── */
    public static final byte[] MAGIC_COOKIE = {99, (byte) 130, 83, 99};

    /* ── Packet fields ── */
    private byte op;
    private byte htype;
    private byte hlen;
    private byte hops;
    private int xid;
    private int secs;
    private int flags;
    private byte[] ciaddr = new byte[4]; /* Client IP */
    private byte[] yiaddr = new byte[4]; /* Your (client) IP — assigned by server */
    private byte[] siaddr = new byte[4]; /* Server IP */
    private byte[] giaddr = new byte[4]; /* Gateway IP */
    private byte[] chaddr = new byte[16]; /* Client hardware address */
    private byte messageType;
    private byte[] requestedIp = new byte[4];
    private byte[] subnetMask = new byte[4];
    private byte[] router = new byte[4];
    private byte[] dnsServer = new byte[4];
    private byte[] serverIdentifier = new byte[4];
    private int leaseTime;

    /**
     * Create a DHCP DISCOVER packet.
     *
     * @param macAddress client MAC address (6 bytes)
     * @return the constructed DhcpPacket
     */
    public static DhcpPacket createDiscover(byte[] macAddress) {
        DhcpPacket packet = new DhcpPacket();
        packet.op = OP_REQUEST;
        packet.htype = HTYPE_ETHERNET;
        packet.hlen = HLEN_ETHERNET;
        packet.hops = 0;
        packet.xid = new Random().nextInt();
        packet.secs = 0;
        packet.flags = 0x8000; /* Broadcast flag */
        System.arraycopy(macAddress, 0, packet.chaddr, 0, Math.min(macAddress.length, 16));
        packet.messageType = DHCP_DISCOVER;
        Logger.info("Created DHCP DISCOVER (xid=0x" + String.format("%08X", packet.xid) + ")");
        return packet;
    }

    /**
     * Create a DHCP OFFER packet in response to a DISCOVER.
     *
     * @param discover     the DISCOVER packet to respond to
     * @param offeredIp    the IP address to offer
     * @param serverIp     the server's IP address
     * @param subnetMask   the subnet mask
     * @param router       the default gateway
     * @param dns          the DNS server
     * @param leaseTime    the lease time in seconds
     * @return the constructed OFFER packet
     */
    public static DhcpPacket createOffer(DhcpPacket discover, String offeredIp,
            String serverIp, String subnetMask, String router, String dns, int leaseTime) {
        DhcpPacket packet = new DhcpPacket();
        packet.op = OP_REPLY;
        packet.htype = HTYPE_ETHERNET;
        packet.hlen = HLEN_ETHERNET;
        packet.xid = discover.xid;
        packet.yiaddr = ByteUtils.ipToBytes(offeredIp);
        packet.siaddr = ByteUtils.ipToBytes(serverIp);
        System.arraycopy(discover.chaddr, 0, packet.chaddr, 0, 16);
        packet.messageType = DHCP_OFFER;
        packet.subnetMask = ByteUtils.ipToBytes(subnetMask);
        packet.router = ByteUtils.ipToBytes(router);
        packet.dnsServer = ByteUtils.ipToBytes(dns);
        packet.serverIdentifier = ByteUtils.ipToBytes(serverIp);
        packet.leaseTime = leaseTime;
        Logger.info("Created DHCP OFFER (IP=" + offeredIp + ")");
        return packet;
    }

    /**
     * Create a DHCP REQUEST packet.
     *
     * @param discover    the original DISCOVER packet
     * @param requestedIp the IP address being requested
     * @param serverIp    the server's IP address
     * @return the constructed REQUEST packet
     */
    public static DhcpPacket createRequest(DhcpPacket discover, String requestedIp, String serverIp) {
        DhcpPacket packet = new DhcpPacket();
        packet.op = OP_REQUEST;
        packet.htype = HTYPE_ETHERNET;
        packet.hlen = HLEN_ETHERNET;
        packet.xid = discover.xid;
        System.arraycopy(discover.chaddr, 0, packet.chaddr, 0, 16);
        packet.messageType = DHCP_REQUEST;
        packet.requestedIp = ByteUtils.ipToBytes(requestedIp);
        packet.serverIdentifier = ByteUtils.ipToBytes(serverIp);
        Logger.info("Created DHCP REQUEST (IP=" + requestedIp + ")");
        return packet;
    }

    /**
     * Create a DHCP ACK packet in response to a REQUEST.
     *
     * @param request      the REQUEST packet to acknowledge
     * @param assignedIp   the confirmed IP address
     * @param serverIp     the server's IP address
     * @param subnetMask   the subnet mask
     * @param router       the default gateway
     * @param dns          the DNS server
     * @param leaseTime    the lease time in seconds
     * @return the constructed ACK packet
     */
    public static DhcpPacket createAck(DhcpPacket request, String assignedIp,
            String serverIp, String subnetMask, String router, String dns, int leaseTime) {
        DhcpPacket packet = new DhcpPacket();
        packet.op = OP_REPLY;
        packet.htype = HTYPE_ETHERNET;
        packet.hlen = HLEN_ETHERNET;
        packet.xid = request.xid;
        packet.yiaddr = ByteUtils.ipToBytes(assignedIp);
        packet.siaddr = ByteUtils.ipToBytes(serverIp);
        System.arraycopy(request.chaddr, 0, packet.chaddr, 0, 16);
        packet.messageType = DHCP_ACK;
        packet.subnetMask = ByteUtils.ipToBytes(subnetMask);
        packet.router = ByteUtils.ipToBytes(router);
        packet.dnsServer = ByteUtils.ipToBytes(dns);
        packet.serverIdentifier = ByteUtils.ipToBytes(serverIp);
        packet.leaseTime = leaseTime;
        Logger.info("Created DHCP ACK (IP=" + assignedIp + ")");
        return packet;
    }

    /**
     * Serialize this DHCP packet to raw bytes.
     *
     * @return the byte array representation
     */
    public byte[] toBytes() {
        byte[] packet = new byte[576]; /* Minimum DHCP packet size */
        int offset = 0;

        packet[offset++] = op;
        packet[offset++] = htype;
        packet[offset++] = hlen;
        packet[offset++] = hops;

        /* XID */
        ByteUtils.writeUint32(packet, offset, xid & 0xFFFFFFFFL);
        offset += 4;

        /* Secs, Flags */
        ByteUtils.writeUint16(packet, offset, secs);
        offset += 2;
        ByteUtils.writeUint16(packet, offset, flags);
        offset += 2;

        /* Addresses */
        System.arraycopy(ciaddr, 0, packet, offset, 4); offset += 4;
        System.arraycopy(yiaddr, 0, packet, offset, 4); offset += 4;
        System.arraycopy(siaddr, 0, packet, offset, 4); offset += 4;
        System.arraycopy(giaddr, 0, packet, offset, 4); offset += 4;

        /* Client hardware address */
        System.arraycopy(chaddr, 0, packet, offset, 16); offset += 16;

        /* Server hostname (64 bytes) and boot filename (128 bytes) — zeroed */
        offset += 64 + 128;

        /* Magic cookie */
        System.arraycopy(MAGIC_COOKIE, 0, packet, offset, 4);
        offset += 4;

        /* Options */
        /* Option 53: DHCP Message Type */
        packet[offset++] = OPT_MSG_TYPE;
        packet[offset++] = 1;
        packet[offset++] = messageType;

        /* Option 50: Requested IP (for REQUEST packets) */
        if (messageType == DHCP_REQUEST && requestedIp != null) {
            packet[offset++] = OPT_REQUESTED_IP;
            packet[offset++] = 4;
            System.arraycopy(requestedIp, 0, packet, offset, 4);
            offset += 4;
        }

        /* Option 54: Server Identifier */
        if (serverIdentifier != null && !isZeroAddress(serverIdentifier)) {
            packet[offset++] = OPT_SERVER_ID;
            packet[offset++] = 4;
            System.arraycopy(serverIdentifier, 0, packet, offset, 4);
            offset += 4;
        }

        /* Option 1: Subnet Mask (for OFFER/ACK) */
        if ((messageType == DHCP_OFFER || messageType == DHCP_ACK) && subnetMask != null) {
            packet[offset++] = OPT_SUBNET_MASK;
            packet[offset++] = 4;
            System.arraycopy(subnetMask, 0, packet, offset, 4);
            offset += 4;
        }

        /* Option 3: Router (for OFFER/ACK) */
        if ((messageType == DHCP_OFFER || messageType == DHCP_ACK) && router != null) {
            packet[offset++] = OPT_ROUTER;
            packet[offset++] = 4;
            System.arraycopy(router, 0, packet, offset, 4);
            offset += 4;
        }

        /* Option 6: DNS Server (for OFFER/ACK) */
        if ((messageType == DHCP_OFFER || messageType == DHCP_ACK) && dnsServer != null) {
            packet[offset++] = OPT_DNS_SERVER;
            packet[offset++] = 4;
            System.arraycopy(dnsServer, 0, packet, offset, 4);
            offset += 4;
        }

        /* Option 51: Lease Time (for OFFER/ACK) */
        if ((messageType == DHCP_OFFER || messageType == DHCP_ACK) && leaseTime > 0) {
            packet[offset++] = OPT_LEASE_TIME;
            packet[offset++] = 4;
            ByteUtils.writeUint32(packet, offset, leaseTime);
            offset += 4;
        }

        /* End option */
        packet[offset] = OPT_END;

        Logger.debug("Serialized DHCP packet (" + offset + " bytes used)");
        return packet;
    }

    /**
     * Decode a raw byte array into a DhcpPacket.
     *
     * @param data   the raw bytes
     * @param length the number of valid bytes
     * @return decoded DhcpPacket
     */
    public static DhcpPacket fromBytes(byte[] data, int length) {
        DhcpPacket packet = new DhcpPacket();
        int offset = 0;

        packet.op = data[offset++];
        packet.htype = data[offset++];
        packet.hlen = data[offset++];
        packet.hops = data[offset++];

        packet.xid = (int) ByteUtils.readUint32(data, offset);
        offset += 4;
        packet.secs = ByteUtils.readUint16(data, offset);
        offset += 2;
        packet.flags = ByteUtils.readUint16(data, offset);
        offset += 2;

        System.arraycopy(data, offset, packet.ciaddr, 0, 4); offset += 4;
        System.arraycopy(data, offset, packet.yiaddr, 0, 4); offset += 4;
        System.arraycopy(data, offset, packet.siaddr, 0, 4); offset += 4;
        System.arraycopy(data, offset, packet.giaddr, 0, 4); offset += 4;
        System.arraycopy(data, offset, packet.chaddr, 0, 16); offset += 16;

        /* Skip sname(64) and file(128) */
        offset += 64 + 128;

        /* Verify magic cookie */
        offset += 4;

        /* Parse options */
        while (offset < length) {
            int optCode = data[offset++] & 0xFF;
            if (optCode == 255) break; /* End */
            if (optCode == 0) continue; /* Padding */

            int optLen = data[offset++] & 0xFF;

            switch (optCode) {
                case 53: /* Message Type */
                    packet.messageType = data[offset];
                    break;
                case 50: /* Requested IP */
                    System.arraycopy(data, offset, packet.requestedIp, 0, 4);
                    break;
                case 54: /* Server Identifier */
                    System.arraycopy(data, offset, packet.serverIdentifier, 0, 4);
                    break;
                case 1: /* Subnet Mask */
                    System.arraycopy(data, offset, packet.subnetMask, 0, 4);
                    break;
                case 3: /* Router */
                    System.arraycopy(data, offset, packet.router, 0, 4);
                    break;
                case 6: /* DNS Server */
                    System.arraycopy(data, offset, packet.dnsServer, 0, 4);
                    break;
                case 51: /* Lease Time */
                    packet.leaseTime = (int) ByteUtils.readUint32(data, offset);
                    break;
                default:
                    /* Skip unknown options */
                    break;
            }
            offset += optLen;
        }

        Logger.debug("Decoded DHCP packet: type=" + messageTypeToString(packet.messageType)
                + " xid=0x" + String.format("%08X", packet.xid));
        return packet;
    }

    /**
     * Check if a 4-byte address is all zeros.
     */
    private static boolean isZeroAddress(byte[] addr) {
        return addr[0] == 0 && addr[1] == 0 && addr[2] == 0 && addr[3] == 0;
    }

    /**
     * Convert a DHCP message type byte to its string representation.
     *
     * @param type the message type byte
     * @return string name of the message type
     */
    public static String messageTypeToString(byte type) {
        switch (type) {
            case DHCP_DISCOVER: return "DISCOVER";
            case DHCP_OFFER:    return "OFFER";
            case DHCP_REQUEST:  return "REQUEST";
            case DHCP_DECLINE:  return "DECLINE";
            case DHCP_ACK:      return "ACK";
            case DHCP_NAK:      return "NAK";
            case DHCP_RELEASE:  return "RELEASE";
            default:            return "UNKNOWN(" + type + ")";
        }
    }

    /* ── Getters ── */

    public byte getOp() { return op; }
    public byte getMessageType() { return messageType; }
    public int getXid() { return xid; }
    public byte[] getYiaddr() { return yiaddr; }
    public byte[] getSiaddr() { return siaddr; }
    public byte[] getChaddr() { return chaddr; }
    public byte[] getSubnetMask() { return subnetMask; }
    public byte[] getRouter() { return router; }
    public byte[] getDnsServer() { return dnsServer; }
    public byte[] getRequestedIp() { return requestedIp; }
    public byte[] getServerIdentifier() { return serverIdentifier; }
    public int getLeaseTime() { return leaseTime; }

    /**
     * Get a human-readable summary of this packet.
     *
     * @return summary string
     */
    public String summary() {
        return String.format("DHCP %s  xid=0x%08X  yiaddr=%s  siaddr=%s",
                messageTypeToString(messageType), xid,
                ByteUtils.bytesToIp(yiaddr, 0),
                ByteUtils.bytesToIp(siaddr, 0));
    }
}
