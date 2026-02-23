package tests;

import protocols.dhcp.DhcpPacket;
import protocols.dhcp.DhcpSimServer;
import protocols.dhcp.DhcpSimClient;
import utils.ByteUtils;
import utils.Logger;

/**
 * Test suite for the DHCP protocol implementation.
 * Tests packet construction, serialization/deserialization, and simulated DORA.
 */
public class DhcpTest {

    private static int passed = 0;
    private static int failed = 0;

    /**
     * Run all DHCP tests.
     */
    public static void main(String[] args) {
        Logger.section("DHCP Protocol Tests");

        testDiscoverCreation();
        testOfferCreation();
        testRequestCreation();
        testAckCreation();
        testSerializeDeserialize();
        testMessageTypeStrings();
        testMagicCookie();
        testSimulatedDora();

        Logger.separator();
        System.out.println("DHCP Tests: " + passed + " passed, " + failed + " failed");
        Logger.separator();
    }

    /* ── Unit Tests ── */

    private static void testDiscoverCreation() {
        byte[] mac = {(byte) 0xAA, (byte) 0xBB, (byte) 0xCC, 0x11, 0x22, 0x33};
        DhcpPacket discover = DhcpPacket.createDiscover(mac);

        assertEqual("Discover op", String.valueOf(DhcpPacket.OP_REQUEST),
                String.valueOf(discover.getOp()));
        assertEqual("Discover type", String.valueOf(DhcpPacket.DHCP_DISCOVER),
                String.valueOf(discover.getMessageType()));
        assertEqual("Discover MAC[0]", "AA",
                String.format("%02X", discover.getChaddr()[0] & 0xFF));
    }

    private static void testOfferCreation() {
        byte[] mac = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
        DhcpPacket discover = DhcpPacket.createDiscover(mac);
        DhcpPacket offer = DhcpPacket.createOffer(discover, "192.168.1.100",
                "192.168.1.1", "255.255.255.0", "192.168.1.1", "8.8.8.8", 86400);

        assertEqual("Offer op", String.valueOf(DhcpPacket.OP_REPLY),
                String.valueOf(offer.getOp()));
        assertEqual("Offer type", String.valueOf(DhcpPacket.DHCP_OFFER),
                String.valueOf(offer.getMessageType()));
        assertEqual("Offer IP", "192.168.1.100",
                ByteUtils.bytesToIp(offer.getYiaddr(), 0));
        assertEqual("Offer lease", "86400", String.valueOf(offer.getLeaseTime()));
    }

    private static void testRequestCreation() {
        byte[] mac = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
        DhcpPacket discover = DhcpPacket.createDiscover(mac);
        DhcpPacket request = DhcpPacket.createRequest(discover, "192.168.1.100", "192.168.1.1");

        assertEqual("Request op", String.valueOf(DhcpPacket.OP_REQUEST),
                String.valueOf(request.getOp()));
        assertEqual("Request type", String.valueOf(DhcpPacket.DHCP_REQUEST),
                String.valueOf(request.getMessageType()));
        assertEqual("Requested IP", "192.168.1.100",
                ByteUtils.bytesToIp(request.getRequestedIp(), 0));
    }

    private static void testAckCreation() {
        byte[] mac = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
        DhcpPacket discover = DhcpPacket.createDiscover(mac);
        DhcpPacket request = DhcpPacket.createRequest(discover, "192.168.1.100", "192.168.1.1");
        DhcpPacket ack = DhcpPacket.createAck(request, "192.168.1.100",
                "192.168.1.1", "255.255.255.0", "192.168.1.1", "8.8.8.8", 86400);

        assertEqual("ACK op", String.valueOf(DhcpPacket.OP_REPLY),
                String.valueOf(ack.getOp()));
        assertEqual("ACK type", String.valueOf(DhcpPacket.DHCP_ACK),
                String.valueOf(ack.getMessageType()));
        assertEqual("ACK IP", "192.168.1.100",
                ByteUtils.bytesToIp(ack.getYiaddr(), 0));
    }

    private static void testSerializeDeserialize() {
        byte[] mac = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, 0x01, 0x02};
        DhcpPacket original = DhcpPacket.createDiscover(mac);

        /* Serialize */
        byte[] bytes = original.toBytes();
        assertThat("Serialized length > 240", bytes.length >= 240);

        /* Deserialize */
        DhcpPacket decoded = DhcpPacket.fromBytes(bytes, bytes.length);
        assertEqual("Round-trip op", String.valueOf(original.getOp()),
                String.valueOf(decoded.getOp()));
        assertEqual("Round-trip type", String.valueOf(original.getMessageType()),
                String.valueOf(decoded.getMessageType()));
        assertEqual("Round-trip xid", String.valueOf(original.getXid()),
                String.valueOf(decoded.getXid()));
        assertEqual("Round-trip MAC",
                String.format("%02X", original.getChaddr()[0] & 0xFF),
                String.format("%02X", decoded.getChaddr()[0] & 0xFF));

        pass("Serialize/Deserialize round-trip successful");
    }

    private static void testMessageTypeStrings() {
        assertEqual("DISCOVER string", "DISCOVER",
                DhcpPacket.messageTypeToString(DhcpPacket.DHCP_DISCOVER));
        assertEqual("OFFER string", "OFFER",
                DhcpPacket.messageTypeToString(DhcpPacket.DHCP_OFFER));
        assertEqual("REQUEST string", "REQUEST",
                DhcpPacket.messageTypeToString(DhcpPacket.DHCP_REQUEST));
        assertEqual("ACK string", "ACK",
                DhcpPacket.messageTypeToString(DhcpPacket.DHCP_ACK));
    }

    private static void testMagicCookie() {
        assertEqual("Magic cookie[0]", "99",
                String.valueOf(DhcpPacket.MAGIC_COOKIE[0] & 0xFF));
        assertEqual("Magic cookie[1]", "130",
                String.valueOf(DhcpPacket.MAGIC_COOKIE[1] & 0xFF));
        assertEqual("Magic cookie[2]", "83",
                String.valueOf(DhcpPacket.MAGIC_COOKIE[2] & 0xFF));
        assertEqual("Magic cookie[3]", "99",
                String.valueOf(DhcpPacket.MAGIC_COOKIE[3] & 0xFF));
    }

    /* ── Integration Test: Simulated DORA ── */

    private static void testSimulatedDora() {
        int serverPort = 9067;

        /* Start the simulated DHCP server */
        DhcpSimServer server = new DhcpSimServer(serverPort);
        server.startInBackground();

        try {
            Thread.sleep(500);

            /* Run the DORA sequence */
            DhcpSimClient client = new DhcpSimClient(serverPort);
            client.runDora();

            pass("Simulated DORA exchange completed successfully");
        } catch (Exception e) {
            fail("Simulated DORA failed: " + e.getMessage());
        } finally {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            server.stop();
        }
    }

    /* ── Assertion helpers ── */

    private static void assertEqual(String name, String expected, String actual) {
        if (expected.equals(actual)) {
            pass(name + " = " + actual);
        } else {
            fail(name + ": expected '" + expected + "' but got '" + actual + "'");
        }
    }

    private static void assertThat(String name, boolean condition) {
        if (condition) {
            pass(name);
        } else {
            fail(name);
        }
    }

    private static void pass(String msg) {
        passed++;
        System.out.println("  ✓ PASS: " + msg);
    }

    private static void fail(String msg) {
        failed++;
        System.out.println("  ✗ FAIL: " + msg);
    }
}
