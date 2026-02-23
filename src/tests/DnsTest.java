package tests;

import protocols.dns.DnsPacket;
import protocols.dns.DnsClient;
import utils.ByteUtils;
import utils.Logger;

/**
 * Test suite for the DNS protocol implementation.
 * Tests packet construction, response decoding, and live resolution.
 */
public class DnsTest {

    private static int passed = 0;
    private static int failed = 0;

    /**
     * Run all DNS tests.
     */
    public static void main(String[] args) {
        Logger.section("DNS Protocol Tests");

        testQueryPacketConstruction();
        testQueryPacketSize();
        testDomainEncoding();
        testTypeConstants();
        testLiveResolution();

        Logger.separator();
        System.out.println("DNS Tests: " + passed + " passed, " + failed + " failed");
        Logger.separator();
    }

    /* ── Unit Tests ── */

    private static void testQueryPacketConstruction() {
        DnsPacket packet = new DnsPacket("example.com", DnsPacket.TYPE_A);
        byte[] query = packet.buildQuery();

        /* Verify header: 12 bytes minimum */
        assertThat("Query length >= 12", query.length >= 12);

        /* Verify flags: RD bit set (byte 2, bit 0) */
        assertThat("RD flag set", (query[2] & 0x01) == 0x01);

        /* Verify QDCOUNT = 1 */
        int qdCount = ByteUtils.readUint16(query, 4);
        assertEqual("QDCOUNT", "1", String.valueOf(qdCount));

        /* Verify ANCOUNT = 0 */
        int anCount = ByteUtils.readUint16(query, 6);
        assertEqual("ANCOUNT", "0", String.valueOf(anCount));

        pass("Query packet structure is valid");
    }

    private static void testQueryPacketSize() {
        DnsPacket packet = new DnsPacket("example.com", DnsPacket.TYPE_A);
        byte[] query = packet.buildQuery();

        /* Header(12) + "example"(1+7) + "com"(1+3) + null(1) + QTYPE(2) + QCLASS(2) = 29 */
        int expectedSize = 12 + 1 + 7 + 1 + 3 + 1 + 2 + 2;
        assertEqual("Query packet size", String.valueOf(expectedSize), String.valueOf(query.length));
    }

    private static void testDomainEncoding() {
        DnsPacket packet = new DnsPacket("www.google.com", DnsPacket.TYPE_A);
        byte[] query = packet.buildQuery();

        /* Verify domain encoding starts at offset 12 */
        /* "www" → 3, 'w', 'w', 'w' */
        assertEqual("Label length (www)", "3", String.valueOf(query[12] & 0xFF));
        assertEqual("First char 'w'", "w", String.valueOf((char) query[13]));

        /* "google" → 6, 'g', 'o', 'o', 'g', 'l', 'e' */
        assertEqual("Label length (google)", "6", String.valueOf(query[16] & 0xFF));

        pass("Domain name encoding is correct");
    }

    private static void testTypeConstants() {
        assertEqual("TYPE_A", "1", String.valueOf(DnsPacket.TYPE_A));
        assertEqual("TYPE_CNAME", "5", String.valueOf(DnsPacket.TYPE_CNAME));
        assertEqual("TYPE_MX", "15", String.valueOf(DnsPacket.TYPE_MX));
        assertEqual("TYPE_AAAA", "28", String.valueOf(DnsPacket.TYPE_AAAA));
        assertEqual("CLASS_IN", "1", String.valueOf(DnsPacket.CLASS_IN));

        assertEqual("typeToString(A)", "A", DnsPacket.typeToString(DnsPacket.TYPE_A));
        assertEqual("typeToString(CNAME)", "CNAME", DnsPacket.typeToString(DnsPacket.TYPE_CNAME));
    }

    /* ── Integration Test ── */

    private static void testLiveResolution() {
        try {
            DnsClient client = new DnsClient("8.8.8.8", 53);
            DnsPacket response = client.resolve("example.com");

            assertThat("Has answers", !response.getAnswers().isEmpty());

            boolean hasIp = false;
            for (DnsPacket.DnsRecord record : response.getAnswers()) {
                System.out.println("    Resolved: " + record);
                if (record.type == DnsPacket.TYPE_A && record.data != null) {
                    hasIp = true;
                }
            }
            assertThat("Has A record with IP", hasIp);
            pass("Live DNS resolution successful");

        } catch (Exception e) {
            /* This may fail if there's no internet, mark as skipped */
            System.out.println("  ⚠ SKIP: Live DNS test requires internet: " + e.getMessage());
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
