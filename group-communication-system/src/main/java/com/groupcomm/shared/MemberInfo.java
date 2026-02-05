package com.groupcomm.shared;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents information about a group member.
 * Used by the coordinator to maintain the group state.
 * 
 * Design Pattern: Value Object Pattern
 */
public class MemberInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String memberId;
    private final String ipAddress;
    private final int port;
    private final LocalDateTime joinTime;
    private LocalDateTime lastPing;
    private boolean isCoordinator;
    
    /**
     * Constructs member information.
     * 
     * @param memberId Unique identifier for the member
     * @param ipAddress IP address of the member
     * @param port Port number of the member
     */
    public MemberInfo(String memberId, String ipAddress, int port) {
        this.memberId = memberId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.joinTime = LocalDateTime.now();
        this.lastPing = LocalDateTime.now();
        this.isCoordinator = false;
    }
    
    public String getMemberId() {
        return memberId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public int getPort() {
        return port;
    }
    
    public LocalDateTime getJoinTime() {
        return joinTime;
    }
    
    public LocalDateTime getLastPing() {
        return lastPing;
    }
    
    public void updateLastPing() {
        this.lastPing = LocalDateTime.now();
    }
    
    public boolean isCoordinator() {
        return isCoordinator;
    }
    
    public void setCoordinator(boolean coordinator) {
        isCoordinator = coordinator;
    }
    
    /**
     * Checks if the member is responsive based on last ping time.
     * 
     * @param timeoutSeconds Timeout in seconds
     * @return true if member responded within timeout
     */
    public boolean isResponsive(int timeoutSeconds) {
        return LocalDateTime.now().minusSeconds(timeoutSeconds).isBefore(lastPing);
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s:%d)%s", 
            memberId, 
            ipAddress, 
            port,
            isCoordinator ? " [COORDINATOR]" : ""
        );
    }
    
    /**
     * Creates a protocol string for transmission.
     */
    public String toProtocolString() {
        return String.format("%s|%s|%d|%s", memberId, ipAddress, port, isCoordinator);
    }
    
    /**
     * Parses member info from protocol string.
     */
    public static MemberInfo fromProtocolString(String protocolString) {
        String[] parts = protocolString.split("\\|");
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid protocol string");
        }
        
        MemberInfo info = new MemberInfo(parts[0], parts[1], Integer.parseInt(parts[2]));
        info.setCoordinator(Boolean.parseBoolean(parts[3]));
        return info;
    }
}