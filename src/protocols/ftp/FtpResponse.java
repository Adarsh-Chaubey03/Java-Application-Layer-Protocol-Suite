package protocols.ftp;

/**
 * FTP response parser per RFC 959.
 * Parses the 3-digit reply code and message from FTP server responses.
 *
 * FTP reply codes:
 *   1xx — Positive Preliminary (action started, expect another reply)
 *   2xx — Positive Completion
 *   3xx — Positive Intermediate (need more input)
 *   4xx — Transient Negative (try again)
 *   5xx — Permanent Negative (error)
 */
public class FtpResponse {

    /** The 3-digit reply code */
    private final int code;

    /** The reply message text */
    private final String message;

    /** The full raw reply line */
    private final String rawReply;

    /**
     * Construct an FtpResponse from a raw reply line.
     *
     * @param rawReply the raw FTP server reply line
     */
    public FtpResponse(String rawReply) {
        this.rawReply = rawReply;
        if (rawReply != null && rawReply.length() >= 3) {
            int parsedCode;
            try {
                parsedCode = Integer.parseInt(rawReply.substring(0, 3));
            } catch (NumberFormatException e) {
                parsedCode = -1;
            }
            this.code = parsedCode;
            this.message = rawReply.length() > 4 ? rawReply.substring(4).trim() : "";
        } else {
            this.code = -1;
            this.message = rawReply != null ? rawReply : "";
        }
    }

    /**
     * Get the 3-digit reply code.
     *
     * @return the reply code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get the reply message text.
     *
     * @return the message portion
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the full raw reply.
     *
     * @return the raw reply string
     */
    public String getRawReply() {
        return rawReply;
    }

    /**
     * Check if the reply indicates success (2xx or 1xx).
     *
     * @return true if successful
     */
    public boolean isSuccess() {
        return code >= 100 && code < 400;
    }

    /**
     * Check if this is a positive completion reply (2xx).
     *
     * @return true if positive completion
     */
    public boolean isComplete() {
        return code >= 200 && code < 300;
    }

    /**
     * Check if this is a multi-line reply continuation.
     *
     * @return true if continuation line (hyphen after code)
     */
    public boolean isMultiLine() {
        return rawReply != null && rawReply.length() >= 4 && rawReply.charAt(3) == '-';
    }

    @Override
    public String toString() {
        return code + " " + message;
    }
}
