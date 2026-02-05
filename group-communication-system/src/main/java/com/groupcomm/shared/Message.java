package com.groupcomm.shared;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a message in the group communication system.
 * This class encapsulates all message-related data including sender, recipient, content, and metadata.
 * 
 * Design Pattern: Value Object Pattern - immutable data holder
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final String senderId;
    private final String recipientId; // null for broadcast
    private final String content;
    private final LocalDateTime timestamp;
    private final MessageType type;
    
    public enum MessageType {
        BROADCAST,      // Message to all members
        PRIVATE,        // Message to specific member
        SYSTEM,         // System notification
        PING,           // Health check
        PONG,           // Health check response
        JOIN,           // Member joined
        LEAVE,          // Member left
        COORDINATOR,    // Coordinator notification
        WHO_REQUEST,    // Request member list
        WHO_RESPONSE    // Member list response
    }
    
    /**
     * Constructs a new Message.
     * 
     * @param senderId ID of the sender
     * @param recipientId ID of the recipient (null for broadcast)
     * @param content Message content
     * @param type Type of message
     */
    public Message(String senderId, String recipientId, String content, MessageType type) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Factory method for broadcast messages.
     */
    public static Message broadcast(String senderId, String content) {
        return new Message(senderId, null, content, MessageType.BROADCAST);
    }
    
    /**
     * Factory method for private messages.
     */
    public static Message privateMessage(String senderId, String recipientId, String content) {
        return new Message(senderId, recipientId, content, MessageType.PRIVATE);
    }
    
    /**
     * Factory method for system messages.
     */
    public static Message system(String content) {
        return new Message("SYSTEM", null, content, MessageType.SYSTEM);
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public String getRecipientId() {
        return recipientId;
    }
    
    public String getContent() {
        return content;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getFormattedTimestamp() {
        return timestamp.format(TIME_FORMATTER);
    }
    
    public boolean isBroadcast() {
        return recipientId == null || type == MessageType.BROADCAST;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s", getFormattedTimestamp(), senderId, content);
    }
    
    /**
     * Formats the message for network transmission.
     * Protocol: TYPE|SENDER|RECIPIENT|CONTENT
     */
    public String toProtocolString() {
        return String.format("%s|%s|%s|%s", 
            type.name(),
            senderId != null ? senderId : "",
            recipientId != null ? recipientId : "",
            content != null ? content : ""
        );
    }
    
    /**
     * Parses a message from protocol string.
     */
    public static Message fromProtocolString(String protocolString) {
        String[] parts = protocolString.split("\\|", 4);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid protocol string: " + protocolString);
        }
        
        MessageType type = MessageType.valueOf(parts[0]);
        String sender = parts[1].isEmpty() ? null : parts[1];
        String recipient = parts[2].isEmpty() ? null : parts[2];
        String content = parts[3];
        
        return new Message(sender, recipient, content, type);
    }
}