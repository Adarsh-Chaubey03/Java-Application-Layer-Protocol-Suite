package protocols.smtp;

/**
 * SMTP command builder per RFC 5321.
 * Constructs properly formatted SMTP commands and parses server replies.
 *
 * SMTP reply format: 3-digit code followed by a space and message text.
 * Multi-line replies use a hyphen instead of space (e.g., "250-Hello").
 */
public class SmtpCommand {

    /** Standard SMTP command strings */
    public static final String HELO      = "HELO";
    public static final String EHLO      = "EHLO";
    public static final String MAIL_FROM = "MAIL FROM";
    public static final String RCPT_TO   = "RCPT TO";
    public static final String DATA      = "DATA";
    public static final String QUIT      = "QUIT";
    public static final String RSET      = "RSET";
    public static final String NOOP      = "NOOP";

    /* ── Reply code ranges ── */

    /** Positive completion (200-299) */
    public static final int REPLY_READY         = 220;
    public static final int REPLY_CLOSING       = 221;
    public static final int REPLY_OK            = 250;
    public static final int REPLY_START_INPUT   = 354;

    /**
     * Build a HELO command.
     *
     * @param domain the client's domain name
     * @return formatted HELO command string
     */
    public static String helo(String domain) {
        return HELO + " " + domain;
    }

    /**
     * Build a MAIL FROM command.
     *
     * @param senderEmail the sender's email address
     * @return formatted MAIL FROM command string
     */
    public static String mailFrom(String senderEmail) {
        return MAIL_FROM + ":<" + senderEmail + ">";
    }

    /**
     * Build a RCPT TO command.
     *
     * @param recipientEmail the recipient's email address
     * @return formatted RCPT TO command string
     */
    public static String rcptTo(String recipientEmail) {
        return RCPT_TO + ":<" + recipientEmail + ">";
    }

    /**
     * Build a DATA command.
     *
     * @return the DATA command string
     */
    public static String data() {
        return DATA;
    }

    /**
     * Build a QUIT command.
     *
     * @return the QUIT command string
     */
    public static String quit() {
        return QUIT;
    }

    /**
     * Build the message body terminator (CRLF.CRLF).
     *
     * @return the DATA termination sequence
     */
    public static String dataTerminator() {
        return "\r\n.\r\n";
    }

    /**
     * Parse the reply code from an SMTP server response line.
     *
     * @param reply the raw server reply string
     * @return the 3-digit reply code, or -1 if parsing fails
     */
    public static int parseReplyCode(String reply) {
        if (reply == null || reply.length() < 3) {
            return -1;
        }
        try {
            return Integer.parseInt(reply.substring(0, 3));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Parse the reply message from an SMTP server response line.
     *
     * @param reply the raw server reply string
     * @return the message portion after the reply code
     */
    public static String parseReplyMessage(String reply) {
        if (reply == null || reply.length() < 4) {
            return "";
        }
        return reply.substring(4).trim();
    }

    /**
     * Check if a reply code indicates success (2xx or 3xx).
     *
     * @param code the reply code
     * @return true if the reply indicates success
     */
    public static boolean isSuccess(int code) {
        return code >= 200 && code < 400;
    }

    /**
     * Check if a reply is a multi-line continuation.
     *
     * @param reply the raw server reply string
     * @return true if this is a continuation line (has hyphen after code)
     */
    public static boolean isMultiLine(String reply) {
        return reply != null && reply.length() >= 4 && reply.charAt(3) == '-';
    }
}
