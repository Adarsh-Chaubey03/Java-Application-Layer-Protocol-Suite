package utils;

/**
 * Runtime configuration holder for the application.
 * Stores global settings like verbose mode, default timeouts,
 * and server addresses. Thread-safe via volatile fields.
 */
public class Config {

    /** Whether verbose (debug) logging is enabled */
    private static volatile boolean verbose = false;

    /** Default connection timeout in milliseconds */
    private static int defaultConnectTimeout = 5000;

    /** Default read timeout in milliseconds */
    private static int defaultReadTimeout = 5000;

    /** Default DNS server address */
    private static String dnsServer = "8.8.8.8";

    /** Default DNS server port */
    private static int dnsPort = 53;

    /** Default HTTP server port for demo */
    private static int httpServerPort = 8080;

    /** Default SMTP server port */
    private static int smtpPort = 25;

    /** Default FTP server port */
    private static int ftpPort = 21;

    /** Default DHCP server port for simulation */
    private static int dhcpServerPort = 6700;

    /** Default DHCP client port for simulation */
    private static int dhcpClientPort = 6800;

    /* ── Getters ── */

    public static boolean isVerbose() {
        return verbose;
    }

    public static int getDefaultConnectTimeout() {
        return defaultConnectTimeout;
    }

    public static int getDefaultReadTimeout() {
        return defaultReadTimeout;
    }

    public static String getDnsServer() {
        return dnsServer;
    }

    public static int getDnsPort() {
        return dnsPort;
    }

    public static int getHttpServerPort() {
        return httpServerPort;
    }

    public static int getSmtpPort() {
        return smtpPort;
    }

    public static int getFtpPort() {
        return ftpPort;
    }

    public static int getDhcpServerPort() {
        return dhcpServerPort;
    }

    public static int getDhcpClientPort() {
        return dhcpClientPort;
    }

    /* ── Setters ── */

    public static void setVerbose(boolean verbose) {
        Config.verbose = verbose;
    }

    public static void setDefaultConnectTimeout(int timeout) {
        Config.defaultConnectTimeout = timeout;
    }

    public static void setDefaultReadTimeout(int timeout) {
        Config.defaultReadTimeout = timeout;
    }

    public static void setDnsServer(String server) {
        Config.dnsServer = server;
    }

    public static void setDnsPort(int port) {
        Config.dnsPort = port;
    }

    public static void setHttpServerPort(int port) {
        Config.httpServerPort = port;
    }

    public static void setSmtpPort(int port) {
        Config.smtpPort = port;
    }

    public static void setFtpPort(int port) {
        Config.ftpPort = port;
    }

    public static void setDhcpServerPort(int port) {
        Config.dhcpServerPort = port;
    }

    public static void setDhcpClientPort(int port) {
        Config.dhcpClientPort = port;
    }

    /**
     * Print current configuration to stdout.
     */
    public static void printConfig() {
        System.out.println("Current Configuration:");
        System.out.println("  Verbose Mode     : " + verbose);
        System.out.println("  Connect Timeout  : " + defaultConnectTimeout + "ms");
        System.out.println("  Read Timeout     : " + defaultReadTimeout + "ms");
        System.out.println("  DNS Server       : " + dnsServer + ":" + dnsPort);
        System.out.println("  HTTP Server Port : " + httpServerPort);
        System.out.println("  SMTP Port        : " + smtpPort);
        System.out.println("  FTP Port         : " + ftpPort);
        System.out.println("  DHCP Ports       : server=" + dhcpServerPort + " client=" + dhcpClientPort);
    }
}
