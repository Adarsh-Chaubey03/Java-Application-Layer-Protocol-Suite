package protocols.http;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an HTTP request message per RFC 2616.
 * Supports parsing raw request text and programmatic construction.
 *
 * Structure:
 *   Request-Line = Method SP Request-URI SP HTTP-Version CRLF
 *   Headers      = *(Header-Name: Header-Value CRLF)
 *   CRLF
 *   Body
 */
public class HttpRequest {

    /** HTTP method (e.g., GET, POST) */
    private String method;

    /** Request URI path (e.g., /index.html) */
    private String path;

    /** HTTP version string (e.g., HTTP/1.1) */
    private String version;

    /** Request headers as ordered key-value pairs */
    private final Map<String, String> headers;

    /** Request body (empty for GET) */
    private String body;

    /**
     * Construct an empty HTTP request.
     */
    public HttpRequest() {
        this.headers = new LinkedHashMap<>();
        this.body = "";
        this.version = "HTTP/1.1";
    }

    /**
     * Construct an HTTP request with a method and path.
     *
     * @param method HTTP method
     * @param path   request URI path
     */
    public HttpRequest(String method, String path) {
        this();
        this.method = method;
        this.path = path;
    }

    /**
     * Parse a raw HTTP request string into an HttpRequest object.
     *
     * @param raw the raw HTTP request text
     * @return parsed HttpRequest
     * @throws IllegalArgumentException if the request format is invalid
     */
    public static HttpRequest parse(String raw) {
        HttpRequest request = new HttpRequest();
        String[] lines = raw.split("\r\n");

        if (lines.length == 0) {
            throw new IllegalArgumentException("Empty HTTP request");
        }

        /* Parse request line: METHOD PATH VERSION */
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 3) {
            throw new IllegalArgumentException("Invalid request line: " + lines[0]);
        }
        request.method = requestLine[0];
        request.path = requestLine[1];
        request.version = requestLine[2];

        /* Parse headers */
        int i = 1;
        for (; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                i++;
                break; /* End of headers */
            }
            int colonIdx = lines[i].indexOf(':');
            if (colonIdx > 0) {
                String key = lines[i].substring(0, colonIdx).trim();
                String value = lines[i].substring(colonIdx + 1).trim();
                request.headers.put(key, value);
            }
        }

        /* Parse body (remaining lines) */
        StringBuilder bodyBuilder = new StringBuilder();
        for (; i < lines.length; i++) {
            bodyBuilder.append(lines[i]);
            if (i < lines.length - 1) bodyBuilder.append("\r\n");
        }
        request.body = bodyBuilder.toString();

        return request;
    }

    /**
     * Serialize this request to a properly formatted HTTP request string.
     *
     * @return the serialized HTTP request
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(path).append(" ").append(version).append("\r\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n");
        if (body != null && !body.isEmpty()) {
            sb.append(body);
        }
        return sb.toString();
    }

    /* ── Getters and Setters ── */

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    /**
     * Add a header to this request.
     *
     * @param key   header name
     * @param value header value
     */
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    @Override
    public String toString() {
        return method + " " + path + " " + version + " [" + headers.size() + " headers]";
    }
}
