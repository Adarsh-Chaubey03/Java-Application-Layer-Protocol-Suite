package core;

import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import utils.Logger;

/**
 * Reusable TCP socket client wrapper.
 * Provides connect, send, receive (line-by-line and bulk), and close operations.
 * All operations include configurable timeouts and robust error handling.
 */
public class SocketClient implements AutoCloseable {

    /** Underlying TCP socket */
    private Socket socket;

    /** Buffered output stream for sending data */
    private BufferedWriter writer;

    /** Buffered input stream for receiving data */
    private BufferedReader reader;

    /** Raw output stream for binary writes */
    private OutputStream rawOut;

    /** Raw input stream for binary reads */
    private InputStream rawIn;

    /** Connection timeout in milliseconds */
    private final int connectTimeout;

    /** Read timeout in milliseconds */
    private final int readTimeout;

    /**
     * Construct a SocketClient with specified timeouts.
     *
     * @param connectTimeout connection timeout in milliseconds
     * @param readTimeout    read/receive timeout in milliseconds
     */
    public SocketClient(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * Construct a SocketClient with default timeouts (5 seconds each).
     */
    public SocketClient() {
        this(5000, 5000);
    }

    /**
     * Connect to a remote host and port.
     *
     * @param host the remote hostname or IP address
     * @param port the remote port number
     * @throws IOException if connection fails or times out
     */
    public void connect(String host, int port) throws IOException {
        Logger.debug("Connecting to " + host + ":" + port + " (timeout=" + connectTimeout + "ms)");
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), connectTimeout);
        socket.setSoTimeout(readTimeout);
        rawOut = socket.getOutputStream();
        rawIn = socket.getInputStream();
        writer = new BufferedWriter(new OutputStreamWriter(rawOut));
        reader = new BufferedReader(new InputStreamReader(rawIn));
        Logger.info("Connected to " + host + ":" + port);
    }

    /**
     * Send a string followed by CRLF (standard for most text-based protocols).
     *
     * @param data the string to send
     * @throws IOException if sending fails
     */
    public void sendLine(String data) throws IOException {
        Logger.debug(">>> " + data);
        writer.write(data + "\r\n");
        writer.flush();
    }

    /**
     * Send raw string data without appending CRLF.
     *
     * @param data the raw string to send
     * @throws IOException if sending fails
     */
    public void sendRaw(String data) throws IOException {
        Logger.debug(">>> (raw) " + data.replace("\r\n", "\\r\\n"));
        writer.write(data);
        writer.flush();
    }

    /**
     * Receive a single line of text (up to CRLF or LF).
     *
     * @return the received line, or null if stream ends
     * @throws IOException if receiving fails or times out
     */
    public String receiveLine() throws IOException {
        String line = reader.readLine();
        if (line != null) {
            Logger.debug("<<< " + line);
        }
        return line;
    }

    /**
     * Receive all available data as a string (reads until stream ends or timeout).
     *
     * @return the received data
     * @throws IOException if receiving fails
     */
    public String receiveAll() throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[4096];
        int bytesRead;
        try {
            while ((bytesRead = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, bytesRead);
            }
        } catch (java.net.SocketTimeoutException e) {
            /* Timeout is expected when reading all available data */
            Logger.debug("Read timeout reached (expected for receiveAll)");
        }
        String data = sb.toString();
        Logger.debug("<<< (all) " + data.length() + " chars received");
        return data;
    }

    /**
     * Get the raw output stream for binary protocol operations.
     *
     * @return the raw OutputStream
     */
    public OutputStream getRawOutputStream() {
        return rawOut;
    }

    /**
     * Get the raw input stream for binary protocol operations.
     *
     * @return the raw InputStream
     */
    public InputStream getRawInputStream() {
        return rawIn;
    }

    /**
     * Get the underlying socket.
     *
     * @return the Socket instance
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Check if the socket is connected and open.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Close the socket and release all resources.
     */
    @Override
    public void close() {
        try {
            if (writer != null) writer.close();
            if (reader != null) reader.close();
            if (socket != null && !socket.isClosed()) socket.close();
            Logger.debug("Socket closed");
        } catch (IOException e) {
            Logger.error("Error closing socket: " + e.getMessage());
        }
    }
}
