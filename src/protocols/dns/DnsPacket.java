package protocols.dns;

import utils.ByteUtils;
import utils.Logger;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DNS packet builder and decoder conforming to RFC 1035.
 *
 * DNS message format:
 *   +---------------------+
 *   |       Header        |  12 bytes
 *   +---------------------+
 *   |      Question       |  variable
 *   +---------------------+
 *   |       Answer        |  variable
 *   +---------------------+
 *   |      Authority      |  variable
 *   +---------------------+
 *   |     Additional      |  variable
 *   +---------------------+
 *
 * Header (12 bytes):
 *   ID(16) | FLAGS(16) | QDCOUNT(16) | ANCOUNT(16) | NSCOUNT(16) | ARCOUNT(16)
 */
public class DnsPacket {

    /* ── Record types ── */
    public static final int TYPE_A     = 1;   /* IPv4 address */
    public static final int TYPE_NS    = 2;   /* Name server */
    public static final int TYPE_CNAME = 5;   /* Canonical name */
    public static final int TYPE_MX    = 15;  /* Mail exchange */
    public static final int TYPE_AAAA  = 28;  /* IPv6 address */
    public static final int CLASS_IN   = 1;   /* Internet class */

    /** Transaction ID */
    private int transactionId;

    /** Query domain name */
    private String queryDomain;

    /** Query type (A, CNAME, etc.) */
    private int queryType;

    /** Parsed answer records */
    private final List<DnsRecord> answers;

    /**
     * Represents a single DNS resource record.
     */
    public static class DnsRecord {
        public String name;
        public int type;
        public int dnsClass;
        public long ttl;
        public String data;

        @Override
        public String toString() {
            return String.format("%-30s %-6s %-5s TTL=%-8d %s",
                    name, typeToString(type), "IN", ttl, data);
        }
    }

    /**
     * Construct a DNS packet for querying the given domain.
     *
     * @param domain    the domain name to query
     * @param queryType the query type (e.g., TYPE_A)
     */
    public DnsPacket(String domain, int queryType) {
        this.queryDomain = domain;
        this.queryType = queryType;
        this.transactionId = new Random().nextInt(0xFFFF);
        this.answers = new ArrayList<>();
    }

    /**
     * Private constructor for decoding.
     */
    private DnsPacket() {
        this.answers = new ArrayList<>();
    }

    /**
     * Build the raw DNS query packet bytes.
     *
     * @return byte array containing the DNS query
     */
    public byte[] buildQuery() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        /* Header (12 bytes) */
        /* Transaction ID */
        baos.write((transactionId >> 8) & 0xFF);
        baos.write(transactionId & 0xFF);

        /* Flags: Standard query, recursion desired */
        baos.write(0x01); /* QR=0, Opcode=0, AA=0, TC=0, RD=1 */
        baos.write(0x00); /* RA=0, Z=0, RCODE=0 */

        /* QDCOUNT = 1 */
        baos.write(0x00);
        baos.write(0x01);

        /* ANCOUNT = 0 */
        baos.write(0x00);
        baos.write(0x00);

        /* NSCOUNT = 0 */
        baos.write(0x00);
        baos.write(0x00);

        /* ARCOUNT = 0 */
        baos.write(0x00);
        baos.write(0x00);

        /* Question section: encode domain name */
        encodeDomainName(baos, queryDomain);

        /* QTYPE */
        baos.write((queryType >> 8) & 0xFF);
        baos.write(queryType & 0xFF);

        /* QCLASS = IN */
        baos.write(0x00);
        baos.write(0x01);

        byte[] packet = baos.toByteArray();
        Logger.debug("Built DNS query (" + packet.length + " bytes) for " + queryDomain);
        Logger.debug("Hex dump:\n" + ByteUtils.hexDump(packet));
        return packet;
    }

    /**
     * Decode a raw DNS response packet.
     *
     * @param data   the raw response bytes
     * @param length the number of valid bytes
     * @return decoded DnsPacket with answers populated
     */
    public static DnsPacket decodeResponse(byte[] data, int length) {
        DnsPacket packet = new DnsPacket();

        /* Parse header */
        packet.transactionId = ByteUtils.readUint16(data, 0);
        int flags = ByteUtils.readUint16(data, 2);
        int qdCount = ByteUtils.readUint16(data, 4);
        int anCount = ByteUtils.readUint16(data, 6);
        int nsCount = ByteUtils.readUint16(data, 8);
        int arCount = ByteUtils.readUint16(data, 10);

        Logger.debug("DNS Response: ID=" + packet.transactionId
                + " flags=0x" + String.format("%04X", flags)
                + " QD=" + qdCount + " AN=" + anCount
                + " NS=" + nsCount + " AR=" + arCount);

        int offset = 12;

        /* Skip question section */
        for (int i = 0; i < qdCount; i++) {
            int[] result = decodeName(data, offset);
            offset = result[0];
            packet.queryDomain = decodeNameString(data, offset - (result[0] - offset), result);
            offset += 4; /* Skip QTYPE and QCLASS */
        }

        /* Parse answer section */
        for (int i = 0; i < anCount; i++) {
            if (offset >= length) break;
            DnsRecord record = new DnsRecord();

            /* Decode name */
            String[] nameResult = decodeNameFull(data, offset);
            record.name = nameResult[0];
            offset = Integer.parseInt(nameResult[1]);

            /* Type, class, TTL, data length */
            record.type = ByteUtils.readUint16(data, offset);
            offset += 2;
            record.dnsClass = ByteUtils.readUint16(data, offset);
            offset += 2;
            record.ttl = ByteUtils.readUint32(data, offset);
            offset += 4;
            int rdLength = ByteUtils.readUint16(data, offset);
            offset += 2;

            /* Parse record data based on type */
            if (record.type == TYPE_A && rdLength == 4) {
                record.data = ByteUtils.bytesToIp(data, offset);
            } else if (record.type == TYPE_CNAME) {
                String[] cnameResult = decodeNameFull(data, offset);
                record.data = cnameResult[0];
            } else if (record.type == TYPE_AAAA && rdLength == 16) {
                record.data = decodeIpv6(data, offset);
            } else if (record.type == TYPE_MX) {
                int preference = ByteUtils.readUint16(data, offset);
                String[] mxResult = decodeNameFull(data, offset + 2);
                record.data = preference + " " + mxResult[0];
            } else {
                record.data = "(" + rdLength + " bytes of type " + record.type + ")";
            }

            offset += rdLength;
            packet.answers.add(record);
            Logger.debug("Answer: " + record);
        }

        return packet;
    }

    /**
     * Encode a domain name into DNS wire format (length-prefixed labels).
     *
     * @param baos   the output stream to write to
     * @param domain the domain name (e.g., "example.com")
     */
    private void encodeDomainName(ByteArrayOutputStream baos, String domain) {
        String[] labels = domain.split("\\.");
        for (String label : labels) {
            baos.write(label.length());
            for (char c : label.toCharArray()) {
                baos.write(c);
            }
        }
        baos.write(0); /* Root label terminator */
    }

    /**
     * Decode a DNS name from wire format, handling compression pointers.
     * Returns [newOffset].
     */
    private static int[] decodeName(byte[] data, int offset) {
        int currentOffset = offset;
        while (data[currentOffset] != 0) {
            int len = data[currentOffset] & 0xFF;
            if ((len & 0xC0) == 0xC0) {
                /* Compression pointer — skip 2 bytes */
                currentOffset += 2;
                return new int[]{currentOffset};
            }
            currentOffset += len + 1;
        }
        currentOffset++; /* Skip root label */
        return new int[]{currentOffset};
    }

    /**
     * Placeholder for name string extraction used in question parsing.
     */
    private static String decodeNameString(byte[] data, int offset, int[] result) {
        String[] full = decodeNameFull(data, offset);
        return full[0];
    }

    /**
     * Fully decode a DNS name from wire format, returning [name, newOffset].
     * Handles compression pointers (0xC0 prefix).
     */
    private static String[] decodeNameFull(byte[] data, int offset) {
        StringBuilder name = new StringBuilder();
        int currentOffset = offset;
        boolean jumped = false;
        int jumpReturnOffset = -1;

        while (true) {
            if (currentOffset >= data.length) break;
            int len = data[currentOffset] & 0xFF;

            if (len == 0) {
                currentOffset++;
                break;
            }

            if ((len & 0xC0) == 0xC0) {
                /* Compression pointer */
                if (!jumped) {
                    jumpReturnOffset = currentOffset + 2;
                }
                int pointer = ((len & 0x3F) << 8) | (data[currentOffset + 1] & 0xFF);
                currentOffset = pointer;
                jumped = true;
                continue;
            }

            if (name.length() > 0) name.append(".");
            currentOffset++;
            for (int i = 0; i < len && currentOffset < data.length; i++) {
                name.append((char) (data[currentOffset] & 0xFF));
                currentOffset++;
            }
        }

        int finalOffset = jumped ? jumpReturnOffset : currentOffset;
        return new String[]{name.toString(), String.valueOf(finalOffset)};
    }

    /**
     * Decode an IPv6 address from 16 bytes.
     */
    private static String decodeIpv6(byte[] data, int offset) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i += 2) {
            if (i > 0) sb.append(":");
            sb.append(String.format("%02x%02x", data[offset + i] & 0xFF, data[offset + i + 1] & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Convert a record type integer to its string name.
     */
    public static String typeToString(int type) {
        switch (type) {
            case TYPE_A:     return "A";
            case TYPE_NS:    return "NS";
            case TYPE_CNAME: return "CNAME";
            case TYPE_MX:    return "MX";
            case TYPE_AAAA:  return "AAAA";
            default:         return "TYPE" + type;
        }
    }

    /* ── Getters ── */

    public int getTransactionId() { return transactionId; }
    public String getQueryDomain() { return queryDomain; }
    public int getQueryType() { return queryType; }
    public List<DnsRecord> getAnswers() { return answers; }
}
