package com.groupcomm.patterns;

import com.groupcomm.shared.Message;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Private message strategy - sends message to a specific member.
 * 
 * Design Pattern: Strategy Pattern Implementation
 */
public class PrivateMessageStrategy implements MessageStrategy {
    
    @Override
    public void sendMessage(Message message, Map<String, PrintWriter> writers, boolean excludeSender) {
        String recipientId = message.getRecipientId();
        
        if (recipientId == null) {
            System.err.println("Cannot send private message: no recipient specified");
            return;
        }
        
        PrintWriter recipientWriter = writers.get(recipientId);
        
        if (recipientWriter == null) {
            System.err.println("Cannot send private message: recipient " + recipientId + " not found");
            // Optionally notify sender
            PrintWriter senderWriter = writers.get(message.getSenderId());
            if (senderWriter != null) {
                Message errorMsg = Message.system("User " + recipientId + " not found");
                senderWriter.println(errorMsg.toProtocolString());
                senderWriter.flush();
            }
            return;
        }
        
        String protocolString = message.toProtocolString();
        
        try {
            // Send to recipient
            recipientWriter.println(protocolString);
            recipientWriter.flush();
            
            // Also send confirmation to sender (unless it's the same person)
            if (!message.getSenderId().equals(recipientId)) {
                PrintWriter senderWriter = writers.get(message.getSenderId());
                if (senderWriter != null) {
                    Message confirmation = new Message(
                        "SYSTEM",
                        message.getSenderId(),
                        "Private message sent to " + recipientId,
                        Message.MessageType.SYSTEM
                    );
                    senderWriter.println(confirmation.toProtocolString());
                    senderWriter.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending private message to " + recipientId + ": " + e.getMessage());
        }
    }
    
    @Override
    public String getStrategyName() {
        return "PRIVATE";
    }
}