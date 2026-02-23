package protocols.smtp;

import core.SocketClient;
import utils.Config;
import utils.Logger;

/**
 * SMTP client that implements the full command sequence:
 *   CONNECT → read greeting → HELO → MAIL FROM → RCPT TO → DATA → message → QUIT
 *
 * Conforms to RFC 5321. Parses 3-digit reply codes from the server.
 * Uses the core SocketClient for TCP communication.
 */
public class SmtpClient {

    /** SMTP server hostname or IP */
    private final String server;

    /** SMTP server port */
    private final int port;

    /** Underlying TCP socket */
    private SocketClient client;

    /**
     * Construct an SMTP client for the given server and port.
     *
     * @param server SMTP server hostname or IP
     * @param port   SMTP server port
     */
    public SmtpClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    /**
     * Construct an SMTP client with default port from config.
     *
     * @param server SMTP server hostname or IP
     */
    public SmtpClient(String server) {
        this(server, Config.getSmtpPort());
    }

    /**
     * Connect to the SMTP server and read the initial greeting.
     *
     * @return the server greeting reply
     * @throws Exception if connection fails
     */
    public String connect() throws Exception {
        Logger.info("Connecting to SMTP server " + server + ":" + port);
        client = new SocketClient(Config.getDefaultConnectTimeout(), Config.getDefaultReadTimeout());
        client.connect(server, port);

        /* Read server greeting (expect 220) */
        String greeting = readReply();
        int code = SmtpCommand.parseReplyCode(greeting);
        if (code != SmtpCommand.REPLY_READY) {
            throw new RuntimeException("Unexpected greeting code: " + code + " — " + greeting);
        }
        Logger.info("SMTP Greeting: " + greeting);
        return greeting;
    }

    /**
     * Send HELO command with the specified domain.
     *
     * @param domain the client's domain name
     * @return the server reply
     * @throws Exception if the command fails
     */
    public String helo(String domain) throws Exception {
        return sendCommand(SmtpCommand.helo(domain), SmtpCommand.REPLY_OK);
    }

    /**
     * Send MAIL FROM command.
     *
     * @param senderEmail the sender's email address
     * @return the server reply
     * @throws Exception if the command fails
     */
    public String mailFrom(String senderEmail) throws Exception {
        return sendCommand(SmtpCommand.mailFrom(senderEmail), SmtpCommand.REPLY_OK);
    }

    /**
     * Send RCPT TO command.
     *
     * @param recipientEmail the recipient's email address
     * @return the server reply
     * @throws Exception if the command fails
     */
    public String rcptTo(String recipientEmail) throws Exception {
        return sendCommand(SmtpCommand.rcptTo(recipientEmail), SmtpCommand.REPLY_OK);
    }

    /**
     * Send DATA command, then send the message body, then send the terminator.
     *
     * @param subject the email subject line
     * @param body    the email body text
     * @param from    the From header value
     * @param to      the To header value
     * @return the server reply after data acceptance
     * @throws Exception if the command fails
     */
    public String sendData(String from, String to, String subject, String body) throws Exception {
        /* Send DATA command — expect 354 */
        String dataReply = sendCommand(SmtpCommand.data(), SmtpCommand.REPLY_START_INPUT);
        Logger.info("Server ready for data: " + dataReply);

        /* Construct the email message with headers */
        StringBuilder message = new StringBuilder();
        message.append("From: ").append(from).append("\r\n");
        message.append("To: ").append(to).append("\r\n");
        message.append("Subject: ").append(subject).append("\r\n");
        message.append("Date: ").append(java.time.ZonedDateTime.now().toString()).append("\r\n");
        message.append("MIME-Version: 1.0").append("\r\n");
        message.append("Content-Type: text/plain; charset=UTF-8").append("\r\n");
        message.append("\r\n"); /* Blank line separates headers from body */
        message.append(body);
        message.append(SmtpCommand.dataTerminator());

        client.sendRaw(message.toString());

        /* Read final reply — expect 250 */
        String reply = readReply();
        int code = SmtpCommand.parseReplyCode(reply);
        if (!SmtpCommand.isSuccess(code)) {
            throw new RuntimeException("Data rejected: " + reply);
        }
        Logger.info("Message accepted: " + reply);
        return reply;
    }

    /**
     * Send QUIT command and close the connection.
     *
     * @return the server reply
     * @throws Exception if the command fails
     */
    public String quit() throws Exception {
        String reply = sendCommand(SmtpCommand.quit(), SmtpCommand.REPLY_CLOSING);
        if (client != null) {
            client.close();
        }
        return reply;
    }

    /**
     * Send a command and verify the expected reply code.
     *
     * @param command      the SMTP command string
     * @param expectedCode the expected reply code
     * @return the full server reply
     * @throws Exception if the reply code doesn't match
     */
    private String sendCommand(String command, int expectedCode) throws Exception {
        client.sendLine(command);
        String reply = readReply();
        int code = SmtpCommand.parseReplyCode(reply);
        if (code != expectedCode) {
            Logger.error("Expected " + expectedCode + " but got " + code + ": " + reply);
            throw new RuntimeException("SMTP error: expected " + expectedCode + ", got: " + reply);
        }
        return reply;
    }

    /**
     * Read a complete SMTP reply (handling multi-line replies).
     *
     * @return the combined reply string
     * @throws Exception if reading fails
     */
    private String readReply() throws Exception {
        StringBuilder fullReply = new StringBuilder();
        String line;
        do {
            line = client.receiveLine();
            if (line == null) {
                throw new RuntimeException("SMTP server disconnected unexpectedly");
            }
            if (fullReply.length() > 0) fullReply.append("\n");
            fullReply.append(line);
        } while (SmtpCommand.isMultiLine(line));

        return fullReply.toString();
    }

    /**
     * Perform a full SMTP email send session (connect through quit).
     * Convenience method that chains all steps.
     *
     * @param from    sender email
     * @param to      recipient email
     * @param subject email subject
     * @param body    email body
     */
    public void sendEmail(String from, String to, String subject, String body) {
        try {
            Logger.section("SMTP Session: " + server + ":" + port);
            connect();
            helo("client.local");
            mailFrom(from);
            rcptTo(to);
            sendData(from, to, subject, body);
            quit();
            Logger.info("Email sent successfully!");
        } catch (Exception e) {
            Logger.error("SMTP session failed: " + e.getMessage());
        } finally {
            if (client != null) client.close();
        }
    }
}
