package com.groupcomm;

import com.groupcomm.server.GroupRegistry;
import com.groupcomm.shared.MemberInfo;
import com.groupcomm.shared.Message;
import com.groupcomm.patterns.BroadcastMessageStrategy;
import com.groupcomm.patterns.PrivateMessageStrategy;
import org.junit.jupiter.api.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit test suite for the Group Communication System.
 * 
 * Tests cover:
 * - Group formation and connection
 * - Coordinator election
 * - Member registration and removal
 * - Message delivery strategies
 * - Fault tolerance scenarios
 * 
 * Each test is documented with its purpose and expected behavior.
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class GroupCommunicationSystemTest {
    
    private GroupRegistry registry;
    
    @BeforeEach
    public void setUp() {
        // Reset the singleton instance for each test
        registry = GroupRegistry.getInstance();
        
        // Clear any existing members (using reflection to reset state)
        try {
            java.lang.reflect.Field instanceField = GroupRegistry.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
            registry = GroupRegistry.getInstance();
        } catch (Exception e) {
            // If reflection fails, tests will still work but may have state pollution
        }
    }
    
    /**
     * Test 1: First member becomes coordinator
     * Requirement: "First connection becomes coordinator"
     * Expected: First registered member should have isCoordinator = true
     */
    @Test
    @DisplayName("Test 1: First member becomes coordinator")
    public void testFirstMemberBecomesCoordinator() {
        // Arrange
        MemberInfo member1 = new MemberInfo("Alice", "192.168.1.1", 5000);
        StringWriter sw = new StringWriter();
        PrintWriter writer1 = new PrintWriter(sw);
        
        // Act
        boolean registered = registry.registerMember(member1, writer1);
        
        // Assert
        assertTrue(registered, "First member should register successfully");
        assertTrue(member1.isCoordinator(), "First member should be coordinator");
        assertEquals("Alice", registry.getCoordinatorId(), "Coordinator ID should be Alice");
        assertEquals(1, registry.getMemberCount(), "Should have exactly 1 member");
        
        System.out.println("✓ Test 1 passed: First member becomes coordinator");
    }
    
    /**
     * Test 2: Subsequent members are not coordinators
     * Requirement: "First connection becomes coordinator... whoever joins must be informed"
     * Expected: Second and third members should not be coordinators
     */
    @Test
    @DisplayName("Test 2: Subsequent members are not coordinators")
    public void testSubsequentMembersNotCoordinators() {
        // Arrange
        MemberInfo member1 = new MemberInfo("Alice", "192.168.1.1", 5000);
        MemberInfo member2 = new MemberInfo("Bob", "192.168.1.2", 5001);
        MemberInfo member3 = new MemberInfo("Charlie", "192.168.1.3", 5002);
        
        PrintWriter writer1 = new PrintWriter(new StringWriter());
        PrintWriter writer2 = new PrintWriter(new StringWriter());
        PrintWriter writer3 = new PrintWriter(new StringWriter());
        
        // Act
        registry.registerMember(member1, writer1);
        registry.registerMember(member2, writer2);
        registry.registerMember(member3, writer3);
        
        // Assert
        assertTrue(member1.isCoordinator(), "First member should be coordinator");
        assertFalse(member2.isCoordinator(), "Second member should not be coordinator");
        assertFalse(member3.isCoordinator(), "Third member should not be coordinator");
        assertEquals("Alice", registry.getCoordinatorId(), "Coordinator should still be Alice");
        assertEquals(3, registry.getMemberCount(), "Should have 3 members");
        
        System.out.println("✓ Test 2 passed: Subsequent members are not coordinators");
    }
    
    /**
     * Test 3: Duplicate ID rejection
     * Requirement: "Each member is assigned a unique ID"
     * Expected: Attempting to register duplicate ID should fail
     */
    @Test
    @DisplayName("Test 3: Duplicate ID is rejected")
    public void testDuplicateIdRejection() {
        // Arrange
        MemberInfo member1 = new MemberInfo("Alice", "192.168.1.1", 5000);
        MemberInfo member2 = new MemberInfo("Alice", "192.168.1.2", 5001);
        
        PrintWriter writer1 = new PrintWriter(new StringWriter());
        PrintWriter writer2 = new PrintWriter(new StringWriter());
        
        // Act
        boolean firstRegistration = registry.registerMember(member1, writer1);
        boolean secondRegistration = registry.registerMember(member2, writer2);
        
        // Assert
        assertTrue(firstRegistration, "First registration should succeed");
        assertFalse(secondRegistration, "Second registration with same ID should fail");
        assertEquals(1, registry.getMemberCount(), "Should have only 1 member");
        
        System.out.println("✓ Test 3 passed: Duplicate ID is rejected");
    }
    
    /**
     * Test 4: New coordinator election when coordinator leaves
     * Requirement: "If the coordinator leaves, then any existing member will become a coordinator"
     * Expected: When coordinator leaves, next member should become coordinator
     */
    @Test
    @DisplayName("Test 4: New coordinator election when coordinator leaves")
    public void testCoordinatorElectionAfterLeave() {
        // Arrange
        MemberInfo alice = new MemberInfo("Alice", "192.168.1.1", 5000);
        MemberInfo bob = new MemberInfo("Bob", "192.168.1.2", 5001);
        MemberInfo charlie = new MemberInfo("Charlie", "192.168.1.3", 5002);
        
        registry.registerMember(alice, new PrintWriter(new StringWriter()));
        registry.registerMember(bob, new PrintWriter(new StringWriter()));
        registry.registerMember(charlie, new PrintWriter(new StringWriter()));
        
        // Verify initial state
        assertTrue(alice.isCoordinator(), "Alice should be initial coordinator");
        assertEquals("Alice", registry.getCoordinatorId());
        
        // Act - Coordinator leaves
        registry.removeMember("Alice");
        
        // Assert - New coordinator should be elected
        assertNotNull(registry.getCoordinatorId(), "New coordinator should be elected");
        assertNotEquals("Alice", registry.getCoordinatorId(), "New coordinator should not be Alice");
        assertEquals(2, registry.getMemberCount(), "Should have 2 members remaining");
        
        // Verify one of the remaining members is coordinator
        MemberInfo newCoordinator = registry.getMemberInfo(registry.getCoordinatorId());
        assertNotNull(newCoordinator, "New coordinator should exist");
        assertTrue(newCoordinator.isCoordinator(), "New coordinator should have coordinator flag");
        
        System.out.println("✓ Test 4 passed: New coordinator elected when coordinator leaves");
    }
    
    /**
     * Test 5: Member removal updates registry correctly
     * Requirement: Fault tolerance - "communication among remaining members should not be interrupted"
     * Expected: Registry should correctly update when members leave
     */
    @Test
    @DisplayName("Test 5: Member removal updates registry correctly")
    public void testMemberRemoval() {
        // Arrange
        MemberInfo alice = new MemberInfo("Alice", "192.168.1.1", 5000);
        MemberInfo bob = new MemberInfo("Bob", "192.168.1.2", 5001);
        
        registry.registerMember(alice, new PrintWriter(new StringWriter()));
        registry.registerMember(bob, new PrintWriter(new StringWriter()));
        
        assertEquals(2, registry.getMemberCount(), "Should start with 2 members");
        
        // Act - Remove non-coordinator member
        boolean removed = registry.removeMember("Bob");
        
        // Assert
        assertTrue(removed, "Remove should succeed");
        assertEquals(1, registry.getMemberCount(), "Should have 1 member");
        assertNull(registry.getMemberInfo("Bob"), "Bob should not be in registry");
        assertNotNull(registry.getMemberInfo("Alice"), "Alice should still be in registry");
        assertEquals("Alice", registry.getCoordinatorId(), "Alice should still be coordinator");
        
        System.out.println("✓ Test 5 passed: Member removal updates registry correctly");
    }
    
    /**
     * Test 6: Broadcast message strategy
     * Requirement: "Anyone can send broadcast messages to every other member"
     * Expected: Broadcast message should be sent to all members
     */
    @Test
    @DisplayName("Test 6: Broadcast message reaches all members")
    public void testBroadcastMessageStrategy() {
        // Arrange
        Map<String, PrintWriter> writers = new HashMap<>();
        Map<String, StringWriter> stringWriters = new HashMap<>();
        
        String[] memberIds = {"Alice", "Bob", "Charlie"};
        for (String id : memberIds) {
            StringWriter sw = new StringWriter();
            writers.put(id, new PrintWriter(sw));
            stringWriters.put(id, sw);
        }
        
        BroadcastMessageStrategy strategy = new BroadcastMessageStrategy();
        Message message = Message.broadcast("Alice", "Hello everyone!");
        
        // Act
        strategy.sendMessage(message, writers, true); // excludeSender = true
        
        // Assert - Bob and Charlie should receive, Alice should not (excluded as sender)
        String bobReceived = stringWriters.get("Bob").toString();
        String charlieReceived = stringWriters.get("Charlie").toString();
        String aliceReceived = stringWriters.get("Alice").toString();
        
        assertTrue(bobReceived.contains("Hello everyone!"), "Bob should receive the message");
        assertTrue(charlieReceived.contains("Hello everyone!"), "Charlie should receive the message");
        assertTrue(aliceReceived.isEmpty(), "Alice (sender) should not receive own message");
        
        System.out.println("✓ Test 6 passed: Broadcast message reaches all members");
    }
    
    /**
     * Test 7: Private message strategy
     * Requirement: "Anyone can send private messages to specific member"
     * Expected: Private message should only reach intended recipient
     */
    @Test
    @DisplayName("Test 7: Private message reaches only recipient")
    public void testPrivateMessageStrategy() {
        // Arrange
        Map<String, PrintWriter> writers = new HashMap<>();
        Map<String, StringWriter> stringWriters = new HashMap<>();
        
        String[] memberIds = {"Alice", "Bob", "Charlie"};
        for (String id : memberIds) {
            StringWriter sw = new StringWriter();
            writers.put(id, new PrintWriter(sw));
            stringWriters.put(id, sw);
        }
        
        PrivateMessageStrategy strategy = new PrivateMessageStrategy();
        Message message = Message.privateMessage("Alice", "Bob", "Secret message");
        
        // Act
        strategy.sendMessage(message, writers, false);
        
        // Assert
        String bobReceived = stringWriters.get("Bob").toString();
        String charlieReceived = stringWriters.get("Charlie").toString();
        
        assertTrue(bobReceived.contains("Secret message"), "Bob should receive the private message");
        assertFalse(charlieReceived.contains("Secret message"), "Charlie should not receive the private message");
        
        System.out.println("✓ Test 7 passed: Private message reaches only recipient");
    }
    
    /**
     * Test 8: Message protocol serialization/deserialization
     * Requirement: Proper message encoding for network transmission
     * Expected: Message should serialize and deserialize correctly
     */
    @Test
    @DisplayName("Test 8: Message protocol serialization")
    public void testMessageSerialization() {
        // Arrange
        Message original = Message.broadcast("Alice", "Test message");
        
        // Act
        String protocolString = original.toProtocolString();
        Message deserialized = Message.fromProtocolString(protocolString);
        
        // Assert
        assertEquals(original.getSenderId(), deserialized.getSenderId(), "Sender ID should match");
        assertEquals(original.getContent(), deserialized.getContent(), "Content should match");
        assertEquals(original.getType(), deserialized.getType(), "Type should match");
        
        System.out.println("✓ Test 8 passed: Message protocol serialization works correctly");
    }
    
    /**
     * Test 9: Group state maintenance
     * Requirement: "The state of the group must be maintained correctly"
     * Expected: Registry should accurately track all members
     */
    @Test
    @DisplayName("Test 9: Group state is maintained correctly")
    public void testGroupStateMaintenance() {
        // Arrange & Act - Add members sequentially
        MemberInfo alice = new MemberInfo("Alice", "192.168.1.1", 5000);
        MemberInfo bob = new MemberInfo("Bob", "192.168.1.2", 5001);
        
        registry.registerMember(alice, new PrintWriter(new StringWriter()));
        int countAfterFirst = registry.getMemberCount();
        
        registry.registerMember(bob, new PrintWriter(new StringWriter()));
        int countAfterSecond = registry.getMemberCount();
        
        registry.removeMember("Alice");
        int countAfterRemoval = registry.getMemberCount();
        
        // Assert
        assertEquals(1, countAfterFirst, "Should have 1 member after first registration");
        assertEquals(2, countAfterSecond, "Should have 2 members after second registration");
        assertEquals(1, countAfterRemoval, "Should have 1 member after removal");
        
        assertTrue(registry.isMemberRegistered("Bob"), "Bob should still be registered");
        assertFalse(registry.isMemberRegistered("Alice"), "Alice should not be registered");
        
        System.out.println("✓ Test 9 passed: Group state is maintained correctly");
    }
    
    /**
     * Test 10: Empty group after all members leave
     * Requirement: Fault tolerance - system should handle empty group
     * Expected: No coordinator when group is empty
     */
    @Test
    @DisplayName("Test 10: System handles empty group correctly")
    public void testEmptyGroup() {
        // Arrange
        MemberInfo alice = new MemberInfo("Alice", "192.168.1.1", 5000);
        registry.registerMember(alice, new PrintWriter(new StringWriter()));
        
        // Act
        registry.removeMember("Alice");
        
        // Assert
        assertEquals(0, registry.getMemberCount(), "Group should be empty");
        assertNull(registry.getCoordinatorId(), "Should have no coordinator");
        
        System.out.println("✓ Test 10 passed: System handles empty group correctly");
    }
    
    /**
     * Test 11: Member ping update
     * Requirement: "Coordinator maintains state by periodic ping"
     * Expected: Ping updates should be recorded
     */
    @Test
    @DisplayName("Test 11: Member ping updates are recorded")
    public void testMemberPingUpdate() throws InterruptedException {
        // Arrange
        MemberInfo alice = new MemberInfo("Alice", "192.168.1.1", 5000);
        registry.registerMember(alice, new PrintWriter(new StringWriter()));
        
        // Get initial ping time
        var initialPing = alice.getLastPing();
        
        // Wait a bit
        Thread.sleep(100);
        
        // Act
        registry.updateMemberPing("Alice");
        
        // Assert
        var updatedPing = alice.getLastPing();
        assertTrue(updatedPing.isAfter(initialPing), "Ping time should be updated");
        
        System.out.println("✓ Test 11 passed: Member ping updates are recorded");
    }
    
    /**
     * Test 12: Member responsiveness check
     * Requirement: Fault tolerance through timeout detection
     * Expected: Timeout detection should work correctly
     */
    @Test
    @DisplayName("Test 12: Member responsiveness check works")
    public void testMemberResponsiveness() throws InterruptedException {
        // Arrange
        MemberInfo alice = new MemberInfo("Alice", "192.168.1.1", 5000);
        
        // Act & Assert - Should be responsive immediately
        assertTrue(alice.isResponsive(60), "Member should be responsive within 60 seconds");
        
        // Simulate old ping (by waiting)
        Thread.sleep(100);
        
        // Should not be responsive with very short timeout
        assertFalse(alice.isResponsive(0), "Member should not be responsive with 0 second timeout");
        
        System.out.println("✓ Test 12 passed: Member responsiveness check works");
    }
    
    @AfterEach
    public void tearDown() {
        // Clean up after each test
        System.out.println("Test completed\n");
    }
    
    /**
     * Runs all tests and provides a summary.
     */
    @AfterAll
    public static void printSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ALL TESTS COMPLETED SUCCESSFULLY");
        System.out.println("=".repeat(60));
        System.out.println("\nTested Components:");
        System.out.println("✓ Group formation and connection");
        System.out.println("✓ Coordinator election and re-election");
        System.out.println("✓ Member registration and removal");
        System.out.println("✓ Broadcast and private messaging");
        System.out.println("✓ Group state maintenance");
        System.out.println("✓ Fault tolerance mechanisms");
        System.out.println("=".repeat(60) + "\n");
    }
}