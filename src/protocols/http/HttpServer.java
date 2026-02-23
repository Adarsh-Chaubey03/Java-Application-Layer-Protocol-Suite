package protocols.http;

import core.ProtocolHandler;
import core.TcpServer;
import utils.Logger;

import java.io.*;
import java.net.Socket;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Simple HTTP/1.1 server that handles GET requests.
 * Implements the ProtocolHandler interface for use with TcpServer.
 * Serves a static HTML welcome page and returns 404 for unknown paths.
 */
public class HttpServer implements ProtocolHandler {

    /** HTML content served for the root path */
    private static final String WELCOME_HTML =
            "<!DOCTYPE html>\n"
            + "<html><head><title>Java Protocol Stack</title></head>\n"
            + "<body>\n"
            + "<h1>Welcome to the Java Application Layer Protocol Stack</h1>\n"
            + "<p>This server was built from scratch using java.net sockets.</p>\n"
            + "<p>Supported protocols: HTTP, DNS, SMTP, FTP, DHCP</p>\n"
            + "</body></html>";

    /** The underlying TCP server */
    private TcpServer tcpServer;

    /** The port this server listens on */
    private final int port;

    /**
     * Construct an HTTP server on the given port.
     *
     * @param port the port to listen on
     */
    public HttpServer(int port) {
        this.port = port;
    }

    /**
     * Start the HTTP server in a background thread.
     */
    public void startInBackground() {
        tcpServer = new TcpServer(port, this);
        tcpServer.startInBackground();
        Logger.info("HTTP Server running on port " + port);
    }

    /**
     * Stop the HTTP server.
     */
    public void stop() {
        if (tcpServer != null) {
            tcpServer.stop();
        }
    }

    /**
     * Handle an incoming HTTP connection.
     * Reads the request, parses it, and sends an appropriate response.
     *
     * @param clientSocket the connected client socket
     */
    @Override
    public void handle(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            /* Read the full request */
            StringBuilder rawRequest = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                rawRequest.append(line).append("\r\n");
            }
            rawRequest.append("\r\n");

            if (rawRequest.toString().trim().isEmpty()) {
                Logger.debug("Empty request received, ignoring");
                return;
            }

            /* Parse the request */
            HttpRequest request = HttpRequest.parse(rawRequest.toString());
            Logger.info("Request: " + request);

            /* Build and send the response */
            HttpResponse response = processRequest(request);
            String serialized = response.serialize();
            out.write(serialized.getBytes());
            out.flush();
            Logger.info("Sent response: " + response.getStatusCode() + " " + response.getReasonPhrase());

        } catch (Exception e) {
            Logger.error("Error handling HTTP request: " + e.getMessage());
        }
    }

    /**
     * Process an HTTP request and return the appropriate response.
     *
     * @param request the parsed HTTP request
     * @return the HTTP response to send
     */
    private HttpResponse processRequest(HttpRequest request) {
        /* Only support GET method */
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            HttpResponse response = new HttpResponse(405, "Method Not Allowed");
            response.addHeader("Content-Type", "text/plain");
            response.addHeader("Server", "JavaProtocolStack/1.0");
            response.addHeader("Date", getHttpDate());
            response.addHeader("Connection", "close");
            response.setBody("405 Method Not Allowed: Only GET is supported.\r\n");
            return response;
        }

        String path = request.getPath();

        if ("/".equals(path) || "/index.html".equals(path)) {
            /* Serve the welcome page */
            HttpResponse response = new HttpResponse(200, "OK");
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            response.addHeader("Server", "JavaProtocolStack/1.0");
            response.addHeader("Date", getHttpDate());
            response.addHeader("Connection", "close");
            response.setBody(WELCOME_HTML);
            return response;
        } else if ("/status".equals(path)) {
            /* Health check endpoint */
            HttpResponse response = new HttpResponse(200, "OK");
            response.addHeader("Content-Type", "application/json");
            response.addHeader("Server", "JavaProtocolStack/1.0");
            response.addHeader("Date", getHttpDate());
            response.addHeader("Connection", "close");
            response.setBody("{\"status\":\"running\",\"protocol\":\"HTTP/1.1\"}\r\n");
            return response;
        } else {
            /* 404 for unknown paths */
            HttpResponse response = new HttpResponse(404, "Not Found");
            response.addHeader("Content-Type", "text/plain");
            response.addHeader("Server", "JavaProtocolStack/1.0");
            response.addHeader("Date", getHttpDate());
            response.addHeader("Connection", "close");
            response.setBody("404 Not Found: " + path + "\r\n");
            return response;
        }
    }

    /**
     * Generate an HTTP-formatted date string per RFC 7231.
     *
     * @return formatted date string
     */
    private String getHttpDate() {
        return ZonedDateTime.now()
                .format(DateTimeFormatter.ofPattern(
                        "EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH));
    }
}
