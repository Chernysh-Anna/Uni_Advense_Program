package com.groupcomm.server;

import com.groupcomm.shared.Message;
import com.groupcomm.shared.MemberInfo;
import com.groupcomm.patterns.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientHandler implements Runnable {
    
    private final Socket socket;
    private final GroupRegistry registry;
    private final MessageStrategy broadcastStrategy;
    private final MessageStrategy privateStrategy;
    
    private String memberId;
    private Scanner input;
    private PrintWriter output;
    
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
    
    private void setupConnection() throws IOException {
        input = new Scanner(socket.getInputStream());
        output = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("[CLIENT-HANDLER] New connection from " + socket.getInetAddress());
    }
    
    private boolean authenticateMember() {
        try {
            output.println("SUBMITID");
            output.flush();
            
            if (!input.hasNextLine()) return false;
            String proposedId = input.nextLine().trim();
            
            if (proposedId.isEmpty()) {
                output.println("ERROR|ID cannot be empty");
                return false;
            }
            
            if (registry.isMemberRegistered(proposedId)) {
                output.println("ERROR|ID " + proposedId + " is already in use");
                return false;
            }
            
            this.memberId = proposedId;
            MemberInfo memberInfo = new MemberInfo(
                memberId,
                socket.getInetAddress().getHostAddress(),
                socket.getPort()
            );
            
            if (!registry.registerMember(memberInfo, output)) {
                output.println("ERROR|Registration failed");
                return false;
            }
            
            boolean isCoordinator = memberInfo.isCoordinator();
            output.println("ACCEPTED|" + memberId + "|" + isCoordinator);
            
            // --- WELCOME & COORDINATOR NOTIFICATION ---
            if (isCoordinator) {
                Message welcome = Message.system(
                    "You are the first member.\n" +
                    "*** YOU ARE THE COORDINATOR ***"
                );
                output.println(welcome.toProtocolString());
            } else {
                String coordId = registry.getCoordinatorId();
                MemberInfo coordInfo = registry.getMemberInfo(coordId);
                String details = (coordInfo != null) ? 
                    coordInfo.getIpAddress() + ":" + coordInfo.getPort() : "Unknown";
                
                Message welcome = Message.system(
                    "Welcome " + memberId + "!\n" +
                    "Current Coordinator: " + coordId + " (" + details + ")"
                );
                output.println(welcome.toProtocolString());
            }
            output.flush();
            
            // Notify others
            Message joinMsg = Message.system(memberId + " has joined.");
            // Exclude sender here so we don't spam them with their own join message immediately
            broadcastStrategy.sendMessage(joinMsg, registry.getAllWriters(), true);
            
            return true;
        } catch (Exception e) {
            System.err.println("Auth error: " + e.getMessage());
            return false;
        }
    }
    
    private void handleClientMessages() {
        while (input.hasNextLine()) {
            try {
                String line = input.nextLine();
                if (line == null || line.trim().isEmpty()) continue;
                
                if (line.startsWith("/")) {
                    handleCommand(line);
                    continue;
                }
                
                Message message = parseClientMessage(line);
                if (message != null) {
                    routeMessage(message);
                }
            } catch (Exception e) {
                System.err.println("Message error: " + e.getMessage());
            }
        }
    }
    
    private Message parseClientMessage(String line) {
        if (line.startsWith("@")) {
            int spaceIndex = line.indexOf(' ');
            if (spaceIndex > 0) {
                String recipient = line.substring(1, spaceIndex);
                String content = line.substring(spaceIndex + 1);
                return Message.privateMessage(memberId, recipient, content);
            }
        }
        if (line.contains("|")) {
            return Message.fromProtocolString(line);
        }
        return Message.broadcast(memberId, line);
    }
    
    private void routeMessage(Message message) {
        System.out.println("[MESSAGE] " + message.toString());
        
        if (message.getType() == Message.MessageType.PONG) {
            registry.updateMemberPing(memberId);
            return;
        }
        
        if (message.getType() == Message.MessageType.PRIVATE) {
            privateStrategy.sendMessage(message, registry.getAllWriters(), false);
        } else {
            // FIX: Changed 'true' to 'false'. 
            // Now the sender ALSO receives the broadcast, confirming it was sent.
            broadcastStrategy.sendMessage(message, registry.getAllWriters(), false);
        }
    }
    
    private void handleCommand(String command) {
        String cmd = command.split(" ", 2)[0].toLowerCase();
        if (cmd.equals("/who") || cmd.equals("/list")) {
            String list = registry.getFormattedMemberList();
            output.println(Message.system(list).toProtocolString());
        } else if (cmd.equals("/quit")) {
            Message goodbye = Message.system("Goodbye " + memberId);
            output.println(goodbye.toProtocolString());
            try { socket.close(); } catch (IOException e) {}
        } else {
            output.println(Message.system("Unknown command").toProtocolString());
        }
        output.flush();
    }
    
    private void cleanup() {
        if (memberId != null) {
            boolean wasCoordinator = false;
            MemberInfo info = registry.getMemberInfo(memberId);
            if (info != null) wasCoordinator = info.isCoordinator();
            
            registry.removeMember(memberId);
            
            Message leaveMsg;
            if (wasCoordinator) {
                String newC = registry.getCoordinatorId();
                leaveMsg = Message.system("Coordinator " + memberId + " left. New Coordinator: " + newC);
            } else {
                leaveMsg = Message.system(memberId + " left.");
            }
            broadcastStrategy.sendMessage(leaveMsg, registry.getAllWriters(), false);
        }
        try { socket.close(); } catch (IOException e) {}
    }
}