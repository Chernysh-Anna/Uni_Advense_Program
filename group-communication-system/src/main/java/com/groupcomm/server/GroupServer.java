package com.groupcomm.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.groupcomm.patterns.BroadcastStrategy;
import com.groupcomm.patterns.MessageStrategy;
import com.groupcomm.shared.Message;


public class GroupServer {
    
    private static final int DEFAULT_PORT = 1234;
   
    private static final int THREAD_POOL_SIZE = 10;
    
    private final int port;
    private final GroupRegistry registry;
    private final BeatMonitor beatMonitor;
    private final ExecutorService executorService;
    
    private ServerSocket serverSocket;
    private volatile boolean running;
    
    // Constructs a server on the specified port
    public GroupServer(int port) {
        this.port = port;
        this.registry = GroupRegistry.getInstance();
        this.beatMonitor = new BeatMonitor(registry);
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.running = false;
    }
    
    // Constructs a server on the default port
    public GroupServer() {
        this(DEFAULT_PORT);
    }
    
    //Opens server socket, startsbeat monitor, accept clients
    public void start() throws IOException {
        if (running) {
            System.out.println("Server is already running");
            return;
        }
        
        serverSocket = new ServerSocket(port);
        running = true;
        
 
        System.out.println("Server started on port: " + port);
        System.out.println("Waiting for users");
        System.out.println();
        
        beatMonitor.start();
        
        acceptClients();
    }
    
    // loop to accept incoming client connections
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
    
    //Closes all connections and shuts down thread pool
    public void stop() {
        if (!running) {
            return;
        }
        
        System.out.println("\n[SERVER] Shutting down...");
        running = false;

        Message shutdownMsg = Message.system("The server is shut down in 10 seconds. Please try to reconnect later.");
        MessageStrategy broadcastStrategy = new BroadcastStrategy();
        broadcastStrategy.sendMessage(shutdownMsg, registry.getAllWriters(), false);

        beatMonitor.stop();
        
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
    
    
  
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
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
        
        final GroupServer server = new GroupServer(port);
        
        //  shutdown hook 
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





