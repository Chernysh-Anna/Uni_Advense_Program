package com.groupcomm.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main server class for the group communication system.
 * Accepts client connections and manages the group infrastructure.
 * 
 * Design Patterns Used:
 * - Singleton (GroupRegistry)
 * - Strategy (MessageStrategy implementations)
 * - Observer (HeartbeatMonitor)
 * 
 * Features:
 * - Multi-threaded client handling
 * - Automatic coordinator election
 * - Fault tolerance through heartbeat monitoring
 * - Thread-safe member registry
 */
public class GroupCommunicationServer {
    
    private static final int DEFAULT_PORT = 8888;
    private static final int THREAD_POOL_SIZE = 50;
    
    private final int port;
    private final GroupRegistry registry;
    private final HeartbeatMonitor heartbeatMonitor;
    private final ExecutorService executorService;
    
    private ServerSocket serverSocket;
    private volatile boolean running;
    
    /**
     * Constructs a server on the specified port.
     * 
     * @param port Port number to listen on
     */
    public GroupCommunicationServer(int port) {
        this.port = port;
        this.registry = GroupRegistry.getInstance();
        this.heartbeatMonitor = new HeartbeatMonitor(registry);
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.running = false;
    }
    
    /**
     * Constructs a server on the default port.
     */
    public GroupCommunicationServer() {
        this(DEFAULT_PORT);
    }
    
    /**
     * Starts the server.
     * Opens server socket, starts heartbeat monitor, and begins accepting clients.
     * 
     * @throws IOException if server cannot start
     */
    public void start() throws IOException {
        if (running) {
            System.out.println("Server is already running");
            return;
        }
        
        serverSocket = new ServerSocket(port);
        running = true;
        
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   Group Communication Server                  ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println("Server started on port: " + port);
        System.out.println("Waiting for clients to connect...");
        System.out.println();
        
        // Start heartbeat monitoring
        heartbeatMonitor.start();
        
        // Accept client connections
        acceptClients();
    }
    
    /**
     * Main loop to accept incoming client connections.
     * Each client is handled in a separate thread from the pool.
     */
    private void acceptClients() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                
                System.out.println("[SERVER] New connection from: " + 
                    clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                
                // Create and submit handler to thread pool
                ClientHandler handler = new ClientHandler(clientSocket, registry);
                executorService.submit(handler);
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("[SERVER] Error accepting client: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Stops the server gracefully.
     * Closes all connections and shuts down thread pool.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        System.out.println("\n[SERVER] Shutting down...");
        running = false;
        
        // Stop heartbeat monitor
        heartbeatMonitor.stop();
        
        // Close server socket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error closing server socket: " + e.getMessage());
        }
        
        // Shutdown thread pool
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("[SERVER] Server stopped");
    }
    
    /**
     * Checks if the server is currently running.
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Gets the port number the server is listening on.
     * 
     * @return Port number
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Main method to start the server.
     * 
     * @param args Command line arguments (optional: port number)
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    System.err.println("Invalid port number. Using default: " + DEFAULT_PORT);
                    port = DEFAULT_PORT;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: " + DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }
        
        final GroupCommunicationServer server = new GroupCommunicationServer(port);
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SERVER] Shutdown signal received");
            server.stop();
        }));
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}





