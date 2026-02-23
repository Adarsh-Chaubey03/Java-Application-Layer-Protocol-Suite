package protocols.http;

import core.SocketClient;
import utils.Config;
import utils.Logger;

/**
 * HTTP client that manually constructs and sends HTTP/1.1 GET requests.
 * Uses the core SocketClient for TCP communication.
 * No built-in HttpURLConnection or third-party libraries are used.
 */
public class HttpClient {

    /** Target host */
    private final String host;

    /** Target port */
    private final int port;

    /**
     * Construct an HTTP client for the given host and port.
     *
     * @param host the target host
     * @param port the target port
     */
    public HttpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Send an HTTP GET request to the specified path and return the response.
     *
     * @param path the request URI (e.g., "/", "/index.html")
     * @return the parsed HttpResponse
     * @throws Exception if the request fails
     */
    public HttpResponse get(String path) throws Exception {
        Logger.info("HTTP GET " + host + ":" + port + path);

        try (SocketClient client = new SocketClient(
                Config.getDefaultConnectTimeout(), Config.getDefaultReadTimeout())) {

            client.connect(host, port);

            /* Build HTTP/1.1 GET request manually */
            HttpRequest request = new HttpRequest("GET", path);
            request.addHeader("Host", host + (port != 80 ? ":" + port : ""));
            request.addHeader("User-Agent", "JavaProtocolStack/1.0");
            request.addHeader("Accept", "*/*");
            request.addHeader("Connection", "close");

            /* Send serialized request */
            String serialized = request.serialize();
            Logger.debug("Sending request:\n" + serialized);
            client.sendRaw(serialized);

            /* Read full response */
            String rawResponse = client.receiveAll();
            Logger.debug("Raw response length: " + rawResponse.length() + " bytes");

            if (rawResponse.isEmpty()) {
                throw new RuntimeException("Empty response received from server");
            }

            /* Parse and return the response */
            HttpResponse response = HttpResponse.parse(rawResponse);
            Logger.info("Response: " + response.getStatusCode() + " " + response.getReasonPhrase());
            return response;
        }
    }

    /**
     * Send an HTTP GET request and print the full response to stdout.
     *
     * @param path the request URI
     */
    public void getAndPrint(String path) {
        try {
            HttpResponse response = get(path);
            Logger.section("HTTP Response");
            System.out.println("Status : " + response.getStatusCode() + " " + response.getReasonPhrase());
            System.out.println("Headers:");
            response.getHeaders().forEach((k, v) -> System.out.println("  " + k + ": " + v));
            System.out.println("Body   :");
            System.out.println(response.getBody());
        } catch (Exception e) {
            Logger.error("HTTP GET failed: " + e.getMessage());
        }
    }
}
