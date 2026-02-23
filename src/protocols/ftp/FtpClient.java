package protocols.ftp;

import core.SocketClient;
import utils.Config;
import utils.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * FTP client implementing basic operations per RFC 959.
 * Supports: CONNECT, USER/PASS authentication, PASV mode, LIST.
 * Uses the core SocketClient for control connection and raw sockets for data.
 */
public class FtpClient {

    /** FTP server hostname or IP */
    private final String server;

    /** FTP server port */
    private final int port;

    /** Control connection socket */
    private SocketClient controlConnection;

    /**
     * Construct an FTP client for the given server and port.
     *
     * @param server FTP server hostname or IP
     * @param port   FTP server port
     */
    public FtpClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    /**
     * Construct an FTP client with default port.
     *
     * @param server FTP server hostname or IP
     */
    public FtpClient(String server) {
        this(server, Config.getFtpPort());
    }

    /**
     * Connect to the FTP server and read the welcome message.
     *
     * @return the welcome FtpResponse
     * @throws Exception if connection fails
     */
    public FtpResponse connect() throws Exception {
        Logger.info("Connecting to FTP server " + server + ":" + port);
        controlConnection = new SocketClient(
                Config.getDefaultConnectTimeout(), Config.getDefaultReadTimeout());
        controlConnection.connect(server, port);

        /* Read welcome message — expect 220 */
        FtpResponse welcome = readResponse();
        if (welcome.getCode() != 220) {
            throw new RuntimeException("Unexpected FTP welcome: " + welcome);
        }
        Logger.info("FTP Welcome: " + welcome.getMessage());
        return welcome;
    }

    /**
     * Authenticate with username and password.
     *
     * @param username the FTP username
     * @param password the FTP password
     * @return the final authentication response
     * @throws Exception if authentication fails
     */
    public FtpResponse login(String username, String password) throws Exception {
        Logger.info("Logging in as: " + username);

        /* Send USER command — expect 331 (password required) */
        controlConnection.sendLine("USER " + username);
        FtpResponse userReply = readResponse();
        if (userReply.getCode() != 331 && userReply.getCode() != 230) {
            throw new RuntimeException("USER command failed: " + userReply);
        }

        /* If 230, already logged in (no password needed) */
        if (userReply.getCode() == 230) {
            Logger.info("Logged in (no password required)");
            return userReply;
        }

        /* Send PASS command — expect 230 (logged in) */
        controlConnection.sendLine("PASS " + password);
        FtpResponse passReply = readResponse();
        if (passReply.getCode() != 230) {
            throw new RuntimeException("PASS command failed: " + passReply);
        }
        Logger.info("Logged in successfully");
        return passReply;
    }

    /**
     * Enter PASV (passive) mode and return the data connection address.
     *
     * @return a string array [host, port] for the data connection
     * @throws Exception if PASV fails
     */
    public String[] enterPassiveMode() throws Exception {
        controlConnection.sendLine("PASV");
        FtpResponse pasvReply = readResponse();
        if (pasvReply.getCode() != 227) {
            throw new RuntimeException("PASV command failed: " + pasvReply);
        }

        /* Parse PASV response: 227 Entering Passive Mode (h1,h2,h3,h4,p1,p2) */
        String msg = pasvReply.getMessage();
        int start = msg.indexOf('(');
        int end = msg.indexOf(')');
        if (start < 0 || end < 0) {
            throw new RuntimeException("Cannot parse PASV response: " + msg);
        }

        String[] parts = msg.substring(start + 1, end).split(",");
        String host = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        int dataPort = Integer.parseInt(parts[4].trim()) * 256 + Integer.parseInt(parts[5].trim());

        Logger.info("PASV: data connection at " + host + ":" + dataPort);
        return new String[]{host, String.valueOf(dataPort)};
    }

    /**
     * Execute LIST command to list files in the current directory.
     * Uses PASV mode for the data connection.
     *
     * @return the directory listing as a string
     * @throws Exception if LIST fails
     */
    public String list() throws Exception {
        Logger.info("Listing directory...");

        /* Enter passive mode first */
        String[] dataAddr = enterPassiveMode();
        String dataHost = dataAddr[0];
        int dataPort = Integer.parseInt(dataAddr[1]);

        /* Open data connection */
        Socket dataSocket = new Socket(dataHost, dataPort);
        dataSocket.setSoTimeout(Config.getDefaultReadTimeout());

        /* Send LIST command on control connection */
        controlConnection.sendLine("LIST");
        FtpResponse listReply = readResponse();
        if (listReply.getCode() != 150 && listReply.getCode() != 125) {
            dataSocket.close();
            throw new RuntimeException("LIST command failed: " + listReply);
        }

        /* Read data from the data connection */
        StringBuilder listing = new StringBuilder();
        try (BufferedReader dataReader = new BufferedReader(
                new InputStreamReader(dataSocket.getInputStream()))) {
            String line;
            while ((line = dataReader.readLine()) != null) {
                listing.append(line).append("\n");
            }
        }
        dataSocket.close();

        /* Read transfer complete message — expect 226 */
        FtpResponse transferComplete = readResponse();
        Logger.info("LIST complete: " + transferComplete.getMessage());

        return listing.toString();
    }

    /**
     * Send QUIT command and close the connection.
     *
     * @return the QUIT response
     * @throws Exception if the command fails
     */
    public FtpResponse quit() throws Exception {
        controlConnection.sendLine("QUIT");
        FtpResponse reply = readResponse();
        controlConnection.close();
        Logger.info("FTP session closed");
        return reply;
    }

    /**
     * Read a complete FTP response (handling multi-line replies).
     *
     * @return the parsed FtpResponse
     * @throws Exception if reading fails
     */
    private FtpResponse readResponse() throws Exception {
        StringBuilder fullReply = new StringBuilder();
        String line;
        do {
            line = controlConnection.receiveLine();
            if (line == null) {
                throw new RuntimeException("FTP server disconnected");
            }
            if (fullReply.length() > 0) fullReply.append("\n");
            fullReply.append(line);
        } while (line.length() >= 4 && line.charAt(3) == '-');

        return new FtpResponse(fullReply.toString());
    }

    /**
     * Perform a complete FTP LIST session and print results.
     *
     * @param username FTP username
     * @param password FTP password
     */
    public void listAndPrint(String username, String password) {
        try {
            Logger.section("FTP Session: " + server + ":" + port);
            connect();
            login(username, password);
            String listing = list();
            Logger.section("Directory Listing");
            System.out.println(listing);
            quit();
        } catch (Exception e) {
            Logger.error("FTP session failed: " + e.getMessage());
        } finally {
            if (controlConnection != null) controlConnection.close();
        }
    }
}
