package com.groupcomm.server;

import com.groupcomm.shared.MemberInfo;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton registry for managing group members.
 * Thread-safe implementation to maintain group state.
 * 
 * Design Pattern: Singleton Pattern
 * Purpose: Ensures a single source of truth for group membership across all server threads.
 */
public class GroupRegistry {
    
    private static volatile GroupRegistry instance;
    
    // Thread-safe collections for concurrent access
    private final Map<String, MemberInfo> members;
    private final Map<String, PrintWriter> writers;
    private String currentCoordinatorId;
    
    /**
     * Private constructor for Singleton pattern.
     */
    private GroupRegistry() {
        this.members = new ConcurrentHashMap<>();
        this.writers = new ConcurrentHashMap<>();
        this.currentCoordinatorId = null;
    }
    
    /**
     * Gets the singleton instance (thread-safe double-checked locking).
     * 
     * @return The singleton GroupRegistry instance
     */
    public static GroupRegistry getInstance() {
        if (instance == null) {
            synchronized (GroupRegistry.class) {
                if (instance == null) {
                    instance = new GroupRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * Registers a new member in the group.
     * If this is the first member, they become the coordinator.
     * 
     * @param memberInfo Member information
     * @param writer PrintWriter for sending messages to this member
     * @return true if member was registered successfully, false if ID already exists
     */
    public synchronized boolean registerMember(MemberInfo memberInfo, PrintWriter writer) {
        String memberId = memberInfo.getMemberId();
        
        // Check for duplicate ID
        if (members.containsKey(memberId)) {
            return false;
        }
        
        // First member becomes coordinator
        if (members.isEmpty()) {
            memberInfo.setCoordinator(true);
            currentCoordinatorId = memberId;
            System.out.println("[REGISTRY] " + memberId + " registered as COORDINATOR");
        } else {
            System.out.println("[REGISTRY] " + memberId + " registered as member");
        }
        
        members.put(memberId, memberInfo);
        writers.put(memberId, writer);
        
        return true;
    }
    
    /**
     * Removes a member from the group.
     * If the member was the coordinator, elects a new one.
     * 
     * @param memberId ID of the member to remove
     * @return true if member was removed, false if member didn't exist
     */
    public synchronized boolean removeMember(String memberId) {
        if (!members.containsKey(memberId)) {
            return false;
        }
        
        boolean wasCoordinator = memberId.equals(currentCoordinatorId);
        
        members.remove(memberId);
        writers.remove(memberId);
        
        System.out.println("[REGISTRY] " + memberId + " removed from group");
        
        // Elect new coordinator if needed
        if (wasCoordinator && !members.isEmpty()) {
            electNewCoordinator();
        } else if (members.isEmpty()) {
            currentCoordinatorId = null;
            System.out.println("[REGISTRY] Group is now empty");
        }
        
        return true;
    }
    
    /**
     * Elects a new coordinator from remaining members.
     * Uses the first member in the registry (arbitrary but deterministic).
     */
    private void electNewCoordinator() {
        // Get first available member
        String newCoordinatorId = members.keySet().iterator().next();
        MemberInfo newCoordinator = members.get(newCoordinatorId);
        
        newCoordinator.setCoordinator(true);
        currentCoordinatorId = newCoordinatorId;
        
        System.out.println("[REGISTRY] " + newCoordinatorId + " elected as new COORDINATOR");
    }
    
    /**
     * Updates the last ping time for a member.
     * 
     * @param memberId ID of the member
     */
    public synchronized void updateMemberPing(String memberId) {
        MemberInfo member = members.get(memberId);
        if (member != null) {
            member.updateLastPing();
        }
    }
    
    /**
     * Gets information about a specific member.
     * 
     * @param memberId ID of the member
     * @return MemberInfo or null if not found
     */
    public MemberInfo getMemberInfo(String memberId) {
        return members.get(memberId);
    }
    
    /**
     * Gets the current coordinator ID.
     * 
     * @return Coordinator ID or null if no coordinator
     */
    public String getCoordinatorId() {
        return currentCoordinatorId;
    }
    
    /**
     * Gets all member information.
     * 
     * @return Unmodifiable map of all members
     */
    public Map<String, MemberInfo> getAllMembers() {
        return Collections.unmodifiableMap(members);
    }
    
    /**
     * Gets all PrintWriters for message broadcasting.
     * 
     * @return Unmodifiable map of all writers
     */
    public Map<String, PrintWriter> getAllWriters() {
        return Collections.unmodifiableMap(writers);
    }
    
    /**
     * Gets a specific member's PrintWriter.
     * 
     * @param memberId ID of the member
     * @return PrintWriter or null if not found
     */
    public PrintWriter getWriter(String memberId) {
        return writers.get(memberId);
    }
    
    /**
     * Checks if a member ID is already registered.
     * 
     * @param memberId ID to check
     * @return true if ID exists
     */
    public boolean isMemberRegistered(String memberId) {
        return members.containsKey(memberId);
    }
    
    /**
     * Gets the number of registered members.
     * 
     * @return Number of members
     */
    public int getMemberCount() {
        return members.size();
    }
    
    /**
     * Gets a formatted list of all members for display.
     * 
     * @return Formatted string with all member information
     */
    public synchronized String getFormattedMemberList() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== GROUP MEMBERS (Total: ").append(members.size()).append(") ===\n");
        
        for (MemberInfo member : members.values()) {
            sb.append(member.toString()).append("\n");
        }
        
        return sb.toString();
    }
}