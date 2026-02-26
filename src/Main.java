import protocols.http.HttpClient;
import protocols.http.HttpServer;
import protocols.dns.DnsClient;
import protocols.smtp.SmtpClient;
import protocols.ftp.FtpClient;
import protocols.dhcp.DhcpSimServer;
import protocols.dhcp.DhcpSimClient;
import utils.Config;
import utils.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Main entry point for the Application Layer Protocol Stack.
 * Provides a command-line interface to select and run individual protocol demos.
 *
 * Usage:
 *   java Main                    — Interactive menu
 *   java Main --protocol http    — Run HTTP demo directly
 *   java Main --verbose          — Enable debug logging
 */
public class Main {

    /** Interactive input reader */
    private static final BufferedReader INPUT =
            new BufferedReader(new InputStreamReader(System.in));

    /**
     * Application entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        /* Parse command-line arguments */
        parseArgs(args);

        printBanner();

        /* Check for direct protocol argument */
        String directProtocol = getArgValue(args, "--protocol");
        if (directProtocol != null) {
            runProtocol(directProtocol);
            return;
        }

        /* Interactive menu loop */
        boolean running = true;
        while (running) {
            printMenu();
            String choice = readInput("Select option: ");
            switch (choice) {
                case "1": runHttp(); break;
                case "2": runDns(); break;
                case "3": runSmtp(); break;
                case "4": runFtp(); break;
                case "5": runDhcp(); break;
                case "6": toggleVerbose(); break;
                case "7": Config.printConfig(); break;
                case "8": configureSettings(); break;
                case "0":
                    running = false;
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
            System.out.println();
        }
    }

    /**
     * Parse command-line arguments for configuration.
     *
     * @param args the command-line arguments
     */
    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--verbose":
                case "-v":
                    Config.setVerbose(true);
                    break;
                case "--dns-server":
                    if (i + 1 < args.length) Config.setDnsServer(args[++i]);
                    break;
                case "--http-port":
                    if (i + 1 < args.length) Config.setHttpServerPort(Integer.parseInt(args[++i]));
                    break;
                case "--timeout":
                    if (i + 1 < args.length) {
                        int timeout = Integer.parseInt(args[++i]);
                        Config.setDefaultConnectTimeout(timeout);
                        Config.setDefaultReadTimeout(timeout);
                    }
                    break;
            }
        }
    }

    /**
     * Get a named argument value from the args array.
     */
    private static String getArgValue(String[] args, String key) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(key)) return args[i + 1];
        }
        return null;
    }

    /**
     * Run a protocol by name (for CLI --protocol argument).
     */
    private static void runProtocol(String protocol) {
        switch (protocol.toLowerCase()) {
            case "http": runHttp(); break;
            case "dns":  runDns(); break;
            case "smtp": runSmtp(); break;
            case "ftp":  runFtp(); break;
            case "dhcp": runDhcp(); break;
            default:
                Logger.error("Unknown protocol: " + protocol);
                System.out.println("Available: http, dns, smtp, ftp, dhcp");
        }
    }

    /**
     * Print the application banner.
     */
    private static void printBanner() {
        Logger.separator();
        System.out.println("  APPLICATION LAYER PROTOCOL STACK");
        System.out.println("  Built from scratch — Java sockets only");
        System.out.println("  Protocols: HTTP | DNS | SMTP | FTP | DHCP");
        Logger.separator();
    }

    /**
     * Print the interactive menu.
     */
    private static void printMenu() {
        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║         PROTOCOL SELECTION            ║");
        System.out.println("╠═══════════════════════════════════════╣");
        System.out.println("║  1. HTTP  (Client + Server)           ║");
        System.out.println("║  2. DNS   (Client)                    ║");
        System.out.println("║  3. SMTP  (Client)                    ║");
        System.out.println("║  4. FTP   (Client)                    ║");
        System.out.println("║  5. DHCP  (Simulated Exchange)        ║");
        System.out.println("║  6. Toggle Verbose Mode [" +
                (Config.isVerbose() ? "ON " : "OFF") + "]         ║");
        System.out.println("║  7. Show Configuration                ║");
        System.out.println("║  8. Configure Settings                ║");
        System.out.println("║  0. Exit                              ║");
        System.out.println("╚═══════════════════════════════════════╝");
    }

    /* ── Protocol Runners ── */

    /**
     * Run the HTTP demo: starts embedded server, sends GET request, displays response.
     */
    private static void runHttp() {
        Logger.section("HTTP Protocol Demo");
        int port = Config.getHttpServerPort();

        /* Start the embedded HTTP server */
        HttpServer server = new HttpServer(port);
        server.startInBackground();
        System.out.println("HTTP server started on port " + port);

        /* Give the server a moment to start */
        sleep(500);

        /* Send a GET request to the server */
        HttpClient client = new HttpClient("127.0.0.1", port);
        client.getAndPrint("/");
        System.out.println();
        client.getAndPrint("/status");
        System.out.println();
        client.getAndPrint("/nonexistent");

        /* Stop the server */
        server.stop();
    }

    /**
     * Run the DNS demo: resolves a domain name and prints results.
     */
    private static void runDns() {
        Logger.section("DNS Protocol Demo");
        String domain = readInput("Enter domain to resolve (default: example.com): ");
        if (domain == null || domain.trim().isEmpty()) {
            domain = "example.com";
        }
        DnsClient client = new DnsClient();
        client.resolveAndPrint(domain.trim());
    }

    /**
     * Run the SMTP demo: connects to an SMTP server and sends a test email.
     */
    private static void runSmtp() {
        Logger.section("SMTP Protocol Demo");
        String server = readInput("Enter SMTP server (default: localhost): ");
        if (server == null || server.trim().isEmpty()) {
            server = "localhost";
        }
        String portStr = readInput("Enter SMTP port (default: " + Config.getSmtpPort() + "): ");
        int port = Config.getSmtpPort();
        if (portStr != null && !portStr.trim().isEmpty()) {
            port = Integer.parseInt(portStr.trim());
        }

        SmtpClient client = new SmtpClient(server.trim(), port);
        client.sendEmail(
                "sender@example.com",
                "recipient@example.com",
                "Test Email from Java Protocol Stack",
                "Hello!\n\nThis email was sent using a custom SMTP client\n"
                        + "built from scratch with raw TCP sockets.\n\nBest regards,\nJava Protocol Stack"
        );
    }

    /**
     * Run the FTP demo: connects to an FTP server and lists the directory.
     */
    private static void runFtp() {
        Logger.section("FTP Protocol Demo");
        String server = readInput("Enter FTP server (default: ftp.dlptest.com): ");
        if (server == null || server.trim().isEmpty()) {
            server = "ftp.dlptest.com";
        }
        String user = readInput("Enter username (default: dlpuser): ");
        if (user == null || user.trim().isEmpty()) {
            user = "dlpuser";
        }
        String pass = readInput("Enter password (default: rNrKYTX9g7z3RgJRmxWuGHbeu): ");
        if (pass == null || pass.trim().isEmpty()) {
            pass = "rNrKYTX9g7z3RgJRmxWuGHbeu";
        }

        FtpClient client = new FtpClient(server.trim());
        client.listAndPrint(user.trim(), pass.trim());
    }

    /**
     * Run the DHCP simulation: starts local server and client for DORA exchange.
     */
    private static void runDhcp() {
        Logger.section("DHCP Protocol Demo (Simulated)");
        int serverPort = Config.getDhcpServerPort();

        /* Start the simulated DHCP server */
        DhcpSimServer server = new DhcpSimServer(serverPort);
        server.startInBackground();
        System.out.println("DHCP simulation server started on port " + serverPort);

        /* Give the server time to bind */
        sleep(500);

        /* Run the DHCP client */
        DhcpSimClient client = new DhcpSimClient(serverPort);
        client.runDora();

        /* Clean up */
        sleep(500);
        server.stop();
    }

    /* ── Configuration ── */

    /**
     * Toggle verbose (debug) mode.
     */
    private static void toggleVerbose() {
        Config.setVerbose(!Config.isVerbose());
        System.out.println("Verbose mode: " + (Config.isVerbose() ? "ON" : "OFF"));
    }

    /**
     * Interactive settings configuration menu.
     */
    private static void configureSettings() {
        System.out.println("\nConfigure Settings:");
        System.out.println("  1. DNS Server");
        System.out.println("  2. HTTP Server Port");
        System.out.println("  3. Timeout (ms)");
        System.out.println("  4. DHCP Ports");
        String choice = readInput("Select setting: ");

        switch (choice) {
            case "1":
                String dns = readInput("DNS Server (current: " + Config.getDnsServer() + "): ");
                if (dns != null && !dns.trim().isEmpty()) Config.setDnsServer(dns.trim());
                break;
            case "2":
                String httpPort = readInput("HTTP Port (current: " + Config.getHttpServerPort() + "): ");
                if (httpPort != null && !httpPort.trim().isEmpty())
                    Config.setHttpServerPort(Integer.parseInt(httpPort.trim()));
                break;
            case "3":
                String timeout = readInput("Timeout in ms (current: " + Config.getDefaultReadTimeout() + "): ");
                if (timeout != null && !timeout.trim().isEmpty()) {
                    int t = Integer.parseInt(timeout.trim());
                    Config.setDefaultConnectTimeout(t);
                    Config.setDefaultReadTimeout(t);
                }
                break;
            case "4":
                String sPort = readInput("DHCP Server Port (current: " + Config.getDhcpServerPort() + "): ");
                if (sPort != null && !sPort.trim().isEmpty())
                    Config.setDhcpServerPort(Integer.parseInt(sPort.trim()));
                break;
            default:
                System.out.println("Invalid setting.");
        }
    }

    /* ── Helpers ── */

    /**
     * Read a line of input from the user.
     */
    private static String readInput(String prompt) {
        System.out.print(prompt);
        try {
            return INPUT.readLine();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Sleep for specified milliseconds (utility).
     */
    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
