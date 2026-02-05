package com.groupcomm.patterns;

import com.groupcomm.shared.Message;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Broadcast message strategy - sends message to all members.
 * 
 * Design Pattern: Strategy Pattern Implementation
 */
public class BroadcastMessageStrategy implements MessageStrategy {
    
    @Override
    public void sendMessage(Message message, Map<String, PrintWriter> writers, boolean excludeSender) {
        String protocolString = message.toProtocolString();
        
        for (Map.Entry<String, PrintWriter> entry : writers.entrySet()) {
            // Skip sender if excludeSender is true
            if (excludeSender && entry.getKey().equals(message.getSenderId())) {
                continue;
            }
            
            PrintWriter writer = entry.getValue();
            try {
                writer.println(protocolString);
                writer.flush();
            } catch (Exception e) {
                System.err.println("Error broadcasting to " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }
    
    @Override
    public String getStrategyName() {
        return "BROADCAST";
    }
}