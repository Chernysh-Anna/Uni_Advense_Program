package com.groupcomm.server;

import com.groupcomm.shared.Message;
import com.groupcomm.shared.MemberInfo;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class BeatMonitor {
    
    private static final int PING_INTERVAL = 20;
    private static final int TIMEOUT = 40; 
    private final GroupRegistry registry;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running;
    
    //Constructor for user monitoring
    public BeatMonitor(GroupRegistry registry) {
        this.registry = registry;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.running = false;
    }
    
    // TO DO: must Sends ping messages every 20 seconds to all members
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        System.out.println("[BEAT] Monitor started (ping every " + PING_INTERVAL + "s)");
        
        // periodic ping 
        scheduler.scheduleAtFixedRate(
            this::sendPingToAllMembers,
            PING_INTERVAL,
            PING_INTERVAL,
            TimeUnit.SECONDS
        );
        
        // periodic timeout check
        scheduler.scheduleAtFixedRate(
            this::checkForTimeouts,
            TIMEOUT,
            PING_INTERVAL,
            TimeUnit.SECONDS
        );
    }
    
    //stops the beating monitoring
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
        System.out.println("[BEAT] Bating monitoring stops");
    }
    
    /**
     * Sends ping messages to all  members
     * Updates member state 
     */
    private void sendPingToAllMembers() {
        Map<String, PrintWriter> writers = registry.getAllWriters();
        
        if (writers.isEmpty()) {
            return;
        }
        
        System.out.println("[BEAT] Sending ping to " + writers.size() + " members");
        
        Message pingMessage = new Message("SERVER", null, "PING", Message.MessageType.PING);
        String protocolString = pingMessage.toProtocolString();
        
        for (Map.Entry<String, PrintWriter> entry : writers.entrySet()) {
            String memberId = entry.getKey();
            PrintWriter writer = entry.getValue();
            
            try {
                writer.println(protocolString);
                writer.flush();
               
                if (!writer.checkError()) {
                    registry.updateMemberPing(memberId);
                } else {
                    System.err.println("[BEAT] Failed to ping " + memberId);
                }
            } catch (Exception e) {
                System.err.println("[BEAT] Failed to ping " + memberId + ": " + e.getMessage());
            }
        }
    }
    
    // Removes members who haven't responded 
    private void checkForTimeouts() {
        Map<String, MemberInfo> members = registry.getAllMembers();
        
        for (Map.Entry<String, MemberInfo> entry : members.entrySet()) {
            String memberId = entry.getKey();
            MemberInfo memberInfo = entry.getValue();
            
            if (!memberInfo.isResponsive(TIMEOUT)) {
                System.err.println("[BEAT] Member " + memberId + " haven't responded within " + TIMEOUT + "s)");
                handleTimeout(memberId);
            }
        }
    }
    
    // removing non-responsive member and notifying the group
    private void handleTimeout(String memberId) {
        MemberInfo memberInfo = registry.getMemberInfo(memberId);
        boolean wasCoordinator = memberInfo != null && memberInfo.isCoordinator();
        

        registry.removeMember(memberId);
        
        if (wasCoordinator) {
            String newCoordinatorId = registry.getCoordinatorId();
            if (newCoordinatorId != null) {
                // notification about new coordinator 
               MemberInfo newCoord = registry.getMemberInfo(newCoordinatorId);
               PrintWriter newCoordWriter = registry.getWriter(newCoordinatorId);
                
               if (newCoordWriter != null) {
                    try {
                        Message coordNotif = Message.system("You are now the COORDINATOR: " + newCoord.toString());
                        newCoordWriter.println(coordNotif.toProtocolString());
                        newCoordWriter.flush();
                    } catch (Exception e) {
                        System.err.println("[BEAT] Failed to notify new coordinator: " + e.getMessage());
                    }
                    
                    Message announcement = Message.system("New COORDINATOR: " + newCoord.toString());
                    broadcastSystemMessage(announcement);
                }
            } else {
                // if group empty
                return;
            }
        } else {
            // Regular member timeout
            Message notification = Message.system(memberId + " disconnected (timeout)");
            broadcastSystemMessage(notification);
        }
    }
    


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
    
    //Checks if the monitor is currently running
    public boolean isRunning() {
        return running;
    }
}






















