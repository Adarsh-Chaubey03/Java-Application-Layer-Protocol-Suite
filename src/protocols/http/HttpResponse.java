package protocols.http;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an HTTP response message per RFC 2616.
 * Supports building responses programmatically and parsing raw response text.
 *
 * Structure:
 *   Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
 *   Headers     = *(Header-Name: Header-Value CRLF)
 *   CRLF
 *   Body
 */
public class HttpResponse {

    /** HTTP version (e.g., HTTP/1.1) */
    private String version;

    /** Status code (e.g., 200, 404) */
    private int statusCode;

    /** Reason phrase (e.g., OK, Not Found) */
    private String reasonPhrase;

    /** Response headers as ordered key-value pairs */
    private final Map<String, String> headers;

    /** Response body */
    private String body;

    /**
     * Construct an HTTP response with default values.
     */
    public HttpResponse() {
        this.version = "HTTP/1.1";
        this.statusCode = 200;
        this.reasonPhrase = "OK";
        this.headers = new LinkedHashMap<>();
        this.body = "";
    }

    /**
     * Construct an HTTP response with a status code and reason.
     *
     * @param statusCode  the HTTP status code
     * @param reasonPhrase the reason phrase
     */
    public HttpResponse(int statusCode, String reasonPhrase) {
        this();
        this.statusCode = statusCode;
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * Parse a raw HTTP response string into an HttpResponse object.
     *
     * @param raw the raw HTTP response text
     * @return parsed HttpResponse
     * @throws IllegalArgumentException if the response format is invalid
     */
    public static HttpResponse parse(String raw) {
        HttpResponse response = new HttpResponse();

        /* Split headers from body at double CRLF */
        int bodyStart = raw.indexOf("\r\n\r\n");
        String headerSection;
        if (bodyStart >= 0) {
            headerSection = raw.substring(0, bodyStart);
            response.body = raw.substring(bodyStart + 4);
        } else {
            headerSection = raw;
            response.body = "";
        }

        String[] lines = headerSection.split("\r\n");
        if (lines.length == 0) {
            throw new IllegalArgumentException("Empty HTTP response");
        }

        /* Parse status line: HTTP/1.1 200 OK */
        String statusLine = lines[0];
        int firstSpace = statusLine.indexOf(' ');
        int secondSpace = statusLine.indexOf(' ', firstSpace + 1);
        if (firstSpace < 0 || secondSpace < 0) {
            throw new IllegalArgumentException("Invalid status line: " + statusLine);
        }
        response.version = statusLine.substring(0, firstSpace);
        response.statusCode = Integer.parseInt(statusLine.substring(firstSpace + 1, secondSpace));
        response.reasonPhrase = statusLine.substring(secondSpace + 1);

        /* Parse headers */
        for (int i = 1; i < lines.length; i++) {
            int colonIdx = lines[i].indexOf(':');
            if (colonIdx > 0) {
                String key = lines[i].substring(0, colonIdx).trim();
                String value = lines[i].substring(colonIdx + 1).trim();
                response.headers.put(key, value);
            }
        }

        return response;
    }

    /**
     * Serialize this response to a properly formatted HTTP response string.
     *
     * @return the serialized HTTP response
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(version).append(" ").append(statusCode).append(" ")
                .append(reasonPhrase).append("\r\n");

        /* Auto-set Content-Length if body is present and header is missing */
        if (body != null && !body.isEmpty() && !headers.containsKey("Content-Length")) {
            headers.put("Content-Length", String.valueOf(body.getBytes().length));
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        if (body != null) {
            sb.append(body);
        }
        return sb.toString();
    }

    /* ── Getters and Setters ── */

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public String getReasonPhrase() { return reasonPhrase; }
    public void setReasonPhrase(String reasonPhrase) { this.reasonPhrase = reasonPhrase; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    /**
     * Add a header to this response.
     *
     * @param key   header name
     * @param value header value
     */
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    @Override
    public String toString() {
        return version + " " + statusCode + " " + reasonPhrase + " [" + headers.size() + " headers]";
    }
}
