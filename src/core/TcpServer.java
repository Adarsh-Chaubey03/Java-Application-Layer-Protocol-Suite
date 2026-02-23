package core;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import utils.Logger;

/**
 * Multi-threaded TCP server base class.
 * Accepts incoming connections and delegates handling to a ProtocolHandler.
 * Supports graceful shutdown via the stop() method.
 */
public class TcpServer {

    /** The port to listen on */
    private final int port;

    /** The protocol handler to delegate connections to */
    private final ProtocolHandler handler;

    /** Server socket for accepting connections */
    private ServerSocket serverSocket;

    /** Flag to control the server loop */
    private volatile boolean running;

    /**
     * Construct a TcpServer on the given port with the given handler.
     *
     * @param port    the port to listen on
     * @param handler the ProtocolHandler to handle each connection
     */
    public TcpServer(int port, ProtocolHandler handler) {
        this.port = port;
        this.handler = handler;
        this.running = false;
    }

    /**
     * Start the server. This method blocks and listens for connections
     * until stop() is called.
     *
     * @throws IOException if the server socket cannot be created
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(1000); // 1-second accept timeout for clean shutdown
        running = true;
        Logger.info("TCP Server started on port " + port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                Logger.info("Accepted connection from "
                        + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

                /* Handle each connection in a separate thread */
                Thread clientThread = new Thread(() -> {
                    try {
                        handler.handle(clientSocket);
                    } catch (Exception e) {
                        Logger.error("Error handling client: " + e.getMessage());
                    } finally {
                        try {
                            if (!clientSocket.isClosed()) {
                                clientSocket.close();
                            }
                        } catch (IOException e) {
                            Logger.error("Error closing client socket: " + e.getMessage());
                        }
                    }
                });
                clientThread.setDaemon(true);
                clientThread.start();

            } catch (SocketTimeoutException e) {
                /* Accept timed out — loop back to check running flag */
            }
        }

        Logger.info("TCP Server on port " + port + " stopped");
    }

    /**
     * Start the server in a background daemon thread.
     * Returns immediately.
     */
    public void startInBackground() {
        Thread serverThread = new Thread(() -> {
            try {
                start();
            } catch (IOException e) {
                Logger.error("Server error: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        /* Give the server a moment to start */
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stop the server gracefully.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Logger.error("Error closing server socket: " + e.getMessage());
        }
        Logger.info("TCP Server stop requested on port " + port);
    }

    /**
     * Check if the server is currently running.
     *
     * @return true if the server is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Get the port this server is bound to.
     *
     * @return the port number
     */
    public int getPort() {
        return port;
    }
}
