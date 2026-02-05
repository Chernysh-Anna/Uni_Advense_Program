package com.groupcomm.server;

import com.groupcomm.shared.Message;
import com.groupcomm.shared.MemberInfo;
import com.groupcomm.patterns.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Handles communication with a single client.
 * Each client connection runs in its own thread.
 * * Design Pattern: Command Pattern (processes different message types)
 * Purpose: Isolates client handling logic and enables concurrent client management.
 */
public class ClientHandler implements Runnable {
    
    private final Socket socket;
    private final GroupRegistry registry;
    private final MessageStrategy broadcastStrategy;
    private final MessageStrategy privateStrategy;
    
    private String memberId;
    private Scanner input;
    private PrintWriter output;
    
    /**
     * Constructs a client handler.
     * * @param socket The client socket connection
     * @param registry The group registry
     */
    public ClientHandler(Socket socket, GroupRegistry registry) {
        this.socket = socket;
        this.registry = registry;
        this.broadcastStrategy = new BroadcastMessageStrategy();
        this.privateStrategy = new PrivateMessageStrategy();
    }
    
    @Override
    public void run() {
        try {
            setupConnection();
            
            if (authenticateMember()) {
                handleClientMessages();
            }
            
        } catch (IOException e) {
            System.err.println("[CLIENT-HANDLER] Error handling client: " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    /**
     * Sets up input/output streams for the client connection.
     * * @throws IOException if stream setup fails
     */
    private void setupConnection() throws IOException {
        input = new Scanner(socket.getInputStream());
        output = new PrintWriter(socket.getOutputStream(), true);
        
        System.out.println("[CLIENT-HANDLER] New connection from " + socket.getInetAddress());
    }
    
    /**
     * Authenticates the member and registers them in the group.
     * Implements the join protocol.
     * * @return true if authentication successful
     */
    private boolean authenticateMember() {
        try {
            // Request member ID
            output.println("SUBMITID");
            output.flush();
            
            if (!input.hasNextLine()) {
                return false;
            }
            
            String proposedId = input.nextLine().trim();
            
            // Validate ID is not empty
            if (proposedId.isEmpty()) {
                output.println("ERROR|ID cannot be empty");
                output.flush();
                return false;
            }
            
            // Check for duplicate ID
            if (registry.isMemberRegistered(proposedId)) {
                output.println("ERROR|ID " + proposedId + " is already in use");
                output.flush();
                return false;
            }
            
            // Register member
            this.memberId = proposedId;
            MemberInfo memberInfo = new MemberInfo(
                memberId,
                socket.getInetAddress().getHostAddress(),
                socket.getPort()
            );
            
            if (!registry.registerMember(memberInfo, output)) {
                output.println("ERROR|Registration failed");
                output.flush();
                return false;
            }
            
            // Send acceptance
            boolean isCoordinator = memberInfo.isCoordinator();
            output.println("ACCEPTED|" + memberId + "|" + isCoordinator);
            output.flush();
            
            // --- MODIFIED SECTION START ---
            
            // Check status and send appropriate welcome message
            if (isCoordinator) {
                // Case 1: First user is informed they are the coordinator
                Message welcome = Message.system(
                    "You are the first member to join.\n" +
                    "*** YOU ARE NOW THE GROUP COORDINATOR ***\n" +
                    "You are responsible for maintaining the group state."
                );
                output.println(welcome.toProtocolString());
                output.flush();
            } else {
                // Case 2: Subsequent users receive details of the current coordinator
                String coordinatorId = registry.getCoordinatorId();
                MemberInfo coordInfo = registry.getMemberInfo(coordinatorId);
                
                String coordDetails = "Unknown";
                if (coordInfo != null) {
                    coordDetails = String.format("%s (IP: %s, Port: %d)", 
                        coordInfo.getMemberId(), 
                        coordInfo.getIpAddress(), 
                        coordInfo.getPort());
                }

                Message welcome = Message.system(
                    "Welcome to the group, " + memberId + "!\n" +
                    "Current Coordinator: " + coordinatorId + "\n" +
                    "Coordinator Details: " + coordDetails
                );
                output.println(welcome.toProtocolString());
                output.flush();
            }
            
            // --- MODIFIED SECTION END ---
            
            // Announce to other members
            Message joinAnnouncement = Message.system(memberId + " has joined the group");
            broadcastStrategy.sendMessage(joinAnnouncement, registry.getAllWriters(), true);
            
            System.out.println("[CLIENT-HANDLER] " + memberId + " authenticated successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("[CLIENT-HANDLER] Authentication error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Main message handling loop.
     * Processes different message types and commands.
     */
    private void handleClientMessages() {
        System.out.println("[CLIENT-HANDLER] Starting message loop for " + memberId);
        
        while (input.hasNextLine()) {
            try {
                String line = input.nextLine();
                
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                
                // Handle special commands
                if (line.startsWith("/")) {
                    handleCommand(line);
                    continue;
                }
                
                // Parse message
                Message message = parseClientMessage(line);
                
                if (message != null) {
                    routeMessage(message);
                }
                
            } catch (Exception e) {
                System.err.println("[CLIENT-HANDLER] Error processing message from " + memberId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Parses a message from the client.
     * Supports both protocol strings and plain text.
     * * @param line The input line
     * @return Parsed message or null if invalid
     */
    private Message parseClientMessage(String line) {
        try {
            // Check if it's a private message (@recipient message)
            if (line.startsWith("@")) {
                int spaceIndex = line.indexOf(' ');
                if (spaceIndex > 0) {
                    String recipient = line.substring(1, spaceIndex);
                    String content = line.substring(spaceIndex + 1);
                    return Message.privateMessage(memberId, recipient, content);
                }
            }
            
            // Try to parse as protocol string
            if (line.contains("|")) {
                return Message.fromProtocolString(line);
            }
            
            // Treat as broadcast message
            return Message.broadcast(memberId, line);
            
        } catch (Exception e) {
            System.err.println("[CLIENT-HANDLER] Failed to parse message: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Routes a message to the appropriate delivery strategy.
     * * @param message The message to route
     */
    private void routeMessage(Message message) {
        // Log the message
        System.out.println("[MESSAGE] " + message.toString());
        
        // Handle PONG (response to PING)
        if (message.getType() == Message.MessageType.PONG) {
            registry.updateMemberPing(memberId);
            return;
        }
        
        // Route based on message type
        if (message.getType() == Message.MessageType.PRIVATE) {
            privateStrategy.sendMessage(message, registry.getAllWriters(), false);
        } else {
            broadcastStrategy.sendMessage(message, registry.getAllWriters(), true);
        }
    }
    
    /**
     * Handles special commands starting with /.
     * * @param command The command string
     */
    private void handleCommand(String command) {
        String[] parts = command.split(" ", 2);
        String cmd = parts[0].toLowerCase();
        
        switch (cmd) {
            case "/who":
            case "/list":
                handleWhoCommand();
                break;
                
            case "/quit":
            case "/exit":
                handleQuitCommand();
                break;
                
            case "/help":
                handleHelpCommand();
                break;
                
            default:
                Message error = Message.system("Unknown command: " + cmd + ". Type /help for help.");
                output.println(error.toProtocolString());
                output.flush();
        }
    }
    
    /**
     * Handles /who command - lists all group members.
     */
    private void handleWhoCommand() {
        String memberList = registry.getFormattedMemberList();
        Message response = Message.system(memberList);
        output.println(response.toProtocolString());
        output.flush();
    }
    
    /**
     * Handles /quit command - graceful disconnection.
     */
    private void handleQuitCommand() {
        Message goodbye = Message.system("Goodbye, " + memberId);
        output.println(goodbye.toProtocolString());
        output.flush();
        
        // Close connection
        try {
            socket.close();
        } catch (IOException e) {
            // Ignore
        }
    }
    
    /**
     * Handles /help command - displays available commands.
     */
    private void handleHelpCommand() {
        StringBuilder help = new StringBuilder();
        help.append("Available commands:\n");
        help.append("/who or /list - Show all group members\n");
        help.append("/quit or /exit - Leave the group\n");
        help.append("/help - Show this help\n");
        help.append("@username message - Send private message\n");
        help.append("Regular text - Broadcast to all members\n");
        
        Message helpMsg = Message.system(help.toString());
        output.println(helpMsg.toProtocolString());
        output.flush();
    }
    
    /**
     * Cleanup when client disconnects.
     * Implements fault tolerance by properly handling disconnections.
     */
    private void cleanup() {
        if (memberId != null) {
            boolean wasCoordinator = false;
            MemberInfo memberInfo = registry.getMemberInfo(memberId);
            if (memberInfo != null) {
                wasCoordinator = memberInfo.isCoordinator();
            }
            
            // Remove from registry
            registry.removeMember(memberId);
            
            // Announce departure
            Message leaveMsg;
            if (wasCoordinator) {
                String newCoordinatorId = registry.getCoordinatorId();
                leaveMsg = Message.system(
                    "COORDINATOR " + memberId + " has left. " +
                    (newCoordinatorId != null ? newCoordinatorId + " is the new COORDINATOR." : "Group is empty.")
                );
            } else {
                leaveMsg = Message.system(memberId + " has left the group");
            }
            
            broadcastStrategy.sendMessage(leaveMsg, registry.getAllWriters(), false);
            
            System.out.println("[CLIENT-HANDLER] " + memberId + " disconnected");
        }
        
        // Close resources
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }
}