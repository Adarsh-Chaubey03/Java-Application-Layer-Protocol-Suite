package protocols.dns;

import core.UdpClient;
import utils.Config;
import utils.Logger;

import java.net.DatagramPacket;

/**
 * DNS client that builds binary queries and sends them via UDP.
 * Uses configurable DNS server (default: 8.8.8.8:53).
 * No built-in DNS resolver classes are used.
 */
public class DnsClient {

    /** DNS server hostname or IP */
    private final String server;

    /** DNS server port */
    private final int port;

    /**
     * Construct a DNS client with the configured server settings.
     */
    public DnsClient() {
        this.server = Config.getDnsServer();
        this.port = Config.getDnsPort();
    }

    /**
     * Construct a DNS client for a specific server.
     *
     * @param server DNS server hostname or IP
     * @param port   DNS server port
     */
    public DnsClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    /**
     * Resolve a domain name (A record query) and return the decoded packet.
     *
     * @param domain the domain name to resolve (e.g., "example.com")
     * @return the decoded DnsPacket containing answers
     * @throws Exception if the query fails or times out
     */
    public DnsPacket resolve(String domain) throws Exception {
        return resolve(domain, DnsPacket.TYPE_A);
    }

    /**
     * Resolve a domain with a specific query type.
     *
     * @param domain the domain name to resolve
     * @param type   the query type (e.g., DnsPacket.TYPE_A)
     * @return the decoded DnsPacket containing answers
     * @throws Exception if the query fails or times out
     */
    public DnsPacket resolve(String domain, int type) throws Exception {
        Logger.info("DNS Query: " + domain + " (type " + DnsPacket.typeToString(type)
                + ") -> " + server + ":" + port);

        /* Build the query packet */
        DnsPacket queryPacket = new DnsPacket(domain, type);
        byte[] queryBytes = queryPacket.buildQuery();

        /* Send via UDP and receive response */
        try (UdpClient udpClient = new UdpClient(Config.getDefaultReadTimeout())) {
            udpClient.send(queryBytes, server, port);

            DatagramPacket response = udpClient.receive(512);
            Logger.info("Received DNS response: " + response.getLength() + " bytes");

            /* Decode the response */
            DnsPacket responsePacket = DnsPacket.decodeResponse(
                    response.getData(), response.getLength());

            return responsePacket;
        }
    }

    /**
     * Resolve a domain and print the results to stdout.
     *
     * @param domain the domain to resolve
     */
    public void resolveAndPrint(String domain) {
        try {
            DnsPacket response = resolve(domain);
            Logger.section("DNS Results for " + domain);

            if (response.getAnswers().isEmpty()) {
                System.out.println("No records found.");
            } else {
                System.out.println(String.format("%-30s %-6s %-5s %-10s %s",
                        "NAME", "TYPE", "CLASS", "TTL", "DATA"));
                System.out.println("-".repeat(80));
                for (DnsPacket.DnsRecord record : response.getAnswers()) {
                    System.out.println(record);
                }
            }
        } catch (Exception e) {
            Logger.error("DNS resolution failed: " + e.getMessage());
        }
    }
}
