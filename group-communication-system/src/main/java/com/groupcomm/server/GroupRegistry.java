package com.groupcomm.server;

import com.groupcomm.shared.MemberInfo;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GroupRegistry {
    
    private static volatile GroupRegistry instance;
    private final Map<String, MemberInfo> members;
    private final Map<String, PrintWriter> writers;
    private String currentCoordinatorId;
    
    private GroupRegistry() {
        this.members = new ConcurrentHashMap<>();
        this.writers = new ConcurrentHashMap<>();
        this.currentCoordinatorId = null;
    }
   
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
    
    //Registers a new member + check if this is the first member
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
    
    // Removes a member + if  was coordinator, elects a new one.
    public synchronized boolean removeMember(String memberId) {
        if (!members.containsKey(memberId)) {
            return false;
        }
        
        boolean wasCoordinator = memberId.equals(currentCoordinatorId);
        
        members.remove(memberId);
        writers.remove(memberId);
        
        System.out.println("[REGISTRY] " + memberId + " removed from group");
        
        // Elect new coordinator 
        if (wasCoordinator && !members.isEmpty()) {
            electNewCoordinator();
        } else if (members.isEmpty()) {
            currentCoordinatorId = null;
            System.out.println("[REGISTRY] Group is now empty");
        }
        
        return true;
    }
    
    private void electNewCoordinator() {
        String newCoordinatorId = members.keySet().iterator().next();
        MemberInfo newCoordinator = members.get(newCoordinatorId);
        
        newCoordinator.setCoordinator(true);
        currentCoordinatorId = newCoordinatorId;
        System.out.println("[REGISTRY] " + newCoordinatorId + " elected as new COORDINATOR");
    }
    
    //Updates the last ping time for a member.
    public synchronized void updateMemberPing(String memberId) {
        MemberInfo member = members.get(memberId);
        if (member != null) {
            member.updateLastPing();
        }
    }
    
    // Gets information about a specific member.
    public MemberInfo getMemberInfo(String memberId) {
        return members.get(memberId);
    }
    

    public String getCoordinatorId() {
        return currentCoordinatorId;
    }

    public Map<String, MemberInfo> getAllMembers() {
        return Collections.unmodifiableMap(members);
    }
    
    // for message broadcasting.
    public Map<String, PrintWriter> getAllWriters() {
        return Collections.unmodifiableMap(writers);
    }
    
    //Gets a specific member's PrintWriter.
    public PrintWriter getWriter(String memberId) {
        return writers.get(memberId);
    }
    
    // Checks if ID is already registered
    public boolean isMemberRegistered(String memberId) {
        return members.containsKey(memberId);
    }

    //FOR testing
    public int getMemberCount() {
        return members.size();
    }
    
    //formatted list of members 
    public synchronized String getMemberList() {
        StringBuilder sb = new StringBuilder();
        sb.append("Group Members ( ").append(members.size()).append(") \n");
        
        for (MemberInfo member : members.values()) {
            sb.append(member.toString()).append("\n");
        }
        
        return sb.toString();
    }
}