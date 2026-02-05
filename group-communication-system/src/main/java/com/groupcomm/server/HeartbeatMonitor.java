package com.groupcomm.server;

import com.groupcomm.shared.Message;
import com.groupcomm.shared.MemberInfo;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitors member health through periodic ping messages.
 * Implements fault tolerance by detecting unresponsive members.
 * 
 * Design Pattern: Observer Pattern (monitors state changes)
 * Purpose: Maintains group state through periodic health checks every 20 seconds.
 */
public class HeartbeatMonitor {
    
    private static final int PING_INTERVAL_SECONDS = 20;
    private static final int TIMEOUT_SECONDS = 40; // 2x ping interval
    
    private final GroupRegistry registry;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running;
    
    /**
     * Constructs a heartbeat monitor.
     * 
     * @param registry The group registry to monitor
     */
    public HeartbeatMonitor(GroupRegistry registry) {
        this.registry = registry;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.running = false;
    }
    
    /**
     * Starts the heartbeat monitoring.
     * Sends ping messages every 20 seconds to all members.
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        System.out.println("[HEARTBEAT] Monitor started (ping every " + PING_INTERVAL_SECONDS + "s)");
        
        // Schedule periodic ping task
        scheduler.scheduleAtFixedRate(
            this::sendPingToAllMembers,
            PING_INTERVAL_SECONDS,
            PING_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        // Schedule periodic timeout check
        scheduler.scheduleAtFixedRate(
            this::checkForTimeouts,
            PING_INTERVAL_SECONDS * 2,
            PING_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }
    
    /**
     * Stops the heartbeat monitoring.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("[HEARTBEAT] Monitor stopped");
    }
    
    /**
     * Sends ping messages to all registered members.
     * Updates member state based on successful delivery.
     */
    private void sendPingToAllMembers() {
        Map<String, PrintWriter> writers = registry.getAllWriters();
        
        if (writers.isEmpty()) {
            return;
        }
        
        System.out.println("[HEARTBEAT] Sending ping to " + writers.size() + " members");
        
        Message pingMessage = new Message("SERVER", null, "PING", Message.MessageType.PING);
        String protocolString = pingMessage.toProtocolString();
        
        for (Map.Entry<String, PrintWriter> entry : writers.entrySet()) {
            String memberId = entry.getKey();
            PrintWriter writer = entry.getValue();
            
            try {
                writer.println(protocolString);
                writer.flush();
                
                // Check if there was an error (writer.checkError() returns true if there was an error)
                if (!writer.checkError()) {
                    registry.updateMemberPing(memberId);
                } else {
                    System.err.println("[HEARTBEAT] Failed to ping " + memberId + " (connection error)");
                }
            } catch (Exception e) {
                System.err.println("[HEARTBEAT] Failed to ping " + memberId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Checks for members that haven't responded within the timeout period.
     * Removes unresponsive members from the group.
     */
    private void checkForTimeouts() {
        Map<String, MemberInfo> members = registry.getAllMembers();
        
        for (Map.Entry<String, MemberInfo> entry : members.entrySet()) {
            String memberId = entry.getKey();
            MemberInfo memberInfo = entry.getValue();
            
            if (!memberInfo.isResponsive(TIMEOUT_SECONDS)) {
                System.err.println("[HEARTBEAT] Member " + memberId + " timed out (no response for " + TIMEOUT_SECONDS + "s)");
                handleMemberTimeout(memberId);
            }
        }
    }
    
    /**
     * Handles a member timeout by removing them and notifying the group.
     * Implements fault tolerance.
     * 
     * @param memberId ID of the timed-out member
     */
    private void handleMemberTimeout(String memberId) {
        MemberInfo memberInfo = registry.getMemberInfo(memberId);
        boolean wasCoordinator = memberInfo != null && memberInfo.isCoordinator();
        
        // Remove the member
        registry.removeMember(memberId);
        
        // FIXED: Proper coordinator change notification
        if (wasCoordinator) {
            String newCoordinatorId = registry.getCoordinatorId();
            if (newCoordinatorId != null) {
                // Get the new coordinator's details
                MemberInfo newCoord = registry.getMemberInfo(newCoordinatorId);
                
                // Notify the new coordinator directly
                PrintWriter newCoordWriter = registry.getWriter(newCoordinatorId);
                if (newCoordWriter != null) {
                    Message coordNotif = Message.system("You are now the COORDINATOR of this group. Details: " + newCoord.toString());
                    newCoordWriter.println(coordNotif.toProtocolString());
                    newCoordWriter.flush();
                }
                
                // Announce to everyone
                Message announcement = Message.system(newCoordinatorId + " is the new COORDINATOR");
                broadcastSystemMessage(announcement);
            } else {
                // Group is now empty
                return;
            }
        } else {
            // Regular member timeout
            Message notification = Message.system(memberId + " disconnected (timeout)");
            broadcastSystemMessage(notification);
        }
    }
    
    /**
     * Broadcasts a system message to all members.
     * 
     * @param message The system message to broadcast
     */
    private void broadcastSystemMessage(Message message) {
        String protocolString = message.toProtocolString();
        Map<String, PrintWriter> writers = registry.getAllWriters();
        
        for (PrintWriter writer : writers.values()) {
            try {
                writer.println(protocolString);
                writer.flush();
            } catch (Exception e) {
                // Ignore errors during notification
            }
        }
    }
    
    /**
     * Checks if the monitor is currently running.
     * 
     * @return true if running
     */
    public boolean isRunning() {
        return running;
    }
}






















