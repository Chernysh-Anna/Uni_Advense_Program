package com.groupcomm.patterns;

import com.groupcomm.shared.Message;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Strategy Pattern for message delivery.
 * Defines different strategies for sending messages in the group communication system.
 * 
 * Design Pattern: Strategy Pattern
 * Purpose: Allows runtime selection of message delivery algorithms without changing client code.
 */
public interface MessageStrategy {
    
    /**
     * Sends a message according to the strategy implementation.
     * 
     * @param message The message to send
     * @param writers Map of member IDs to their PrintWriters
     * @param excludeSender Whether to exclude the sender from receiving the message
     */
    void sendMessage(Message message, Map<String, PrintWriter> writers, boolean excludeSender);
    
    /**
     * Gets the name of this strategy.
     */
    String getStrategyName();
}