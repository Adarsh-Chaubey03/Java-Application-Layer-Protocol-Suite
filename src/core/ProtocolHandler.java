package core;

import java.net.Socket;

/**
 * Interface for protocol handlers that process incoming client connections.
 * Each protocol server implementation (HTTP, SMTP, FTP, etc.) implements this
 * interface to define how incoming connections are handled.
 *
 * This follows the Strategy pattern, allowing the TcpServer to delegate
 * connection handling to different protocol implementations.
 */
public interface ProtocolHandler {

    /**
     * Handle an incoming client connection.
     *
     * @param clientSocket the connected client socket
     */
    void handle(Socket clientSocket);
}
