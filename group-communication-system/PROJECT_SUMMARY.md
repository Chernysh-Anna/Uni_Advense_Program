
### need to be deleted

### System Components

```
┌─────────────────────────────────────────────────────┐
│                    Client Layer                      │
│  ┌──────────────────────────────────────────────┐  │
│  │  GroupCommunicationClient (Swing GUI)         │  │
│  │  - Connection management                      │  │
│  │  - Message display                            │  │
│  │  - User input handling                        │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────┬───────────────────────────────────┘
                  │ TCP Socket
┌─────────────────┴───────────────────────────────────┐
│                   Server Layer                       │
│  ┌──────────────────────────────────────────────┐  │
│  │  GroupCommunicationServer                     │  │
│  │  - Accepts connections                        │  │
│  │  - Thread pool management                     │  │
│  │  - Heartbeat monitoring                       │  │
│  └──────────────────────────────────────────────┘  │
│                       │                              │
│  ┌────────────────────┴─────────────────────────┐  │
│  │  ClientHandler (one per client)               │  │
│  │  - Authentication                             │  │
│  │  - Message routing                            │  │
│  │  - Command handling                           │  │
│  └──────────────────────────────────────────────┘  │
│                       │                              │
│  ┌────────────────────┴─────────────────────────┐  │
│  │  GroupRegistry (Singleton)                    │  │
│  │  - Member management                          │  │
│  │  - State maintenance                          │  │
│  │  - Coordinator tracking                       │  │
│  └──────────────────────────────────────────────┘  │
│                       │                              │
│  ┌────────────────────┴─────────────────────────┐  │
│  │  HeartbeatMonitor (Observer)                  │  │
│  │  - Periodic health checks                     │  │
│  │  - Timeout detection                          │  │
│  │  - Failure recovery                           │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## 🎯 Key Features

### 1. Dynamic Connection
- User-specified ID, IP, and port via GUI
- Automatic coordinator assignment
- Real-time join notifications
- Duplicate ID prevention

### 2. Intelligent Messaging
- **Broadcast**: Message to all members
  - Syntax: Regular text
  - Example: `Hello everyone!`

- **Private**: Message to specific member
  - Syntax: `@username message`
  - Example: `@Bob Hey there!`

- **Commands**: 
  - `/list` - List all members
  - `/quit` - Leave group

### 3. Fault Tolerance
- **Prevention Layer**: Input validation, thread-safe collections
- **Detection Layer**: Heartbeat monitoring, socket error detection
- **Recovery Layer**: Automatic re-election, member removal, notifications

### 4. Coordinator Management
- First member automatically becomes coordinator
- Coordinator maintains group state
- Automatic re-election on coordinator failure
- All members notified of coordinator changes

### 5. State Maintenance
- Thread-safe member registry (ConcurrentHashMap)
- Timestamped messages (format: [HH:mm:ss])
- Ping tracking for each member
- Real-time state updates

---

## 📐 Design Patterns

### Pattern 1: Singleton (GroupRegistry)
**Purpose**: Single source of truth for group state

**Benefits**:
- Prevents race conditions
- Consistent state across threads
- Easy access from any component

**Implementation**: Thread-safe double-checked locking

---

### Pattern 2: Strategy (Message Delivery)
**Purpose**: Runtime selection of message routing algorithm

**Implementations**:
- `BroadcastMessageStrategy`: Send to all
- `PrivateMessageStrategy`: Send to one

**Benefits**:
- Easy to extend (add new strategies)
- Separates routing from handling
- Follows Open/Closed Principle

---

### Pattern 3: Observer (Heartbeat Monitor)
**Purpose**: Monitor member health and notify on state changes

**Flow**:
1. Monitor observes member state (ping times)
2. Detects state change (timeout)
3. Triggers event (member removal)
4. Notifies observers (remaining members)

**Benefits**:
- Decouples monitoring from server logic
- Asynchronous state change handling
- Clean separation of concerns

---

### Pattern 4: Value Object (Message, MemberInfo)
**Purpose**: Immutable data holders

**Benefits**:
- Thread-safe (immutability)
- Clear data contracts
- Easy serialization

---

## 🛡️ Fault Tolerance Strategy

### Failure Scenarios & Recovery

| Failure Type | Detection | Recovery | Time |
|--------------|-----------|----------|------|
| Client crash | IOException in socket | Remove member, notify group | Immediate |
| Network timeout | Ping failure (40s threshold) | Remove member, notify group | 40 seconds |
| Coordinator crash | Coordinator flag check | Elect new coordinator | Immediate |
| Server restart | N/A (all clients disconnect) | Clients can reconnect | N/A |

### Heartbeat Monitoring
- **Ping Interval**: 20 seconds
- **Timeout Threshold**: 40 seconds (2x ping)
- **Recovery**: Automatic member removal

### Thread Safety
- `ConcurrentHashMap` for member storage
- `synchronized` methods for critical sections
- `SwingUtilities.invokeLater` for GUI updates
- `volatile` fields for visibility

---

## 🧪 Testing Coverage

### Test Suite: 12 Comprehensive Tests

**Group Formation** (3 tests):
- First member becomes coordinator
- Subsequent members not coordinators
- Duplicate ID rejection

**Coordinator Election** (2 tests):
- Coordinator election after leave
- Empty group handling

**State Maintenance** (3 tests):
- Member removal
- Group state maintenance
- Ping updates

**Messaging** (3 tests):
- Broadcast message delivery
- Private message delivery
- Message serialization

**Fault Tolerance** (1 test):
- Member responsiveness check

**Result**: 100% pass rate







---

## 🎓 Learning Outcomes

This project demonstrates mastery of:

1. **Network Programming**: Socket programming, client-server architecture
2. **Concurrency**: Multi-threading, thread pools, synchronization
3. **Design Patterns**: Practical application of 4 major patterns
4. **Fault Tolerance**: Detection, recovery, graceful degradation
5. **GUI Development**: Swing, event handling, thread-safe updates
6. **Testing**: JUnit, unit tests, integration tests
7. **Code Quality**: Documentation, error handling, best practices







## 💡 Key Insights

### Why This Implementation Works

1. **Singleton Pattern**: Prevents state inconsistency across threads
2. **Strategy Pattern**: Makes adding new message types trivial
3. **Observer Pattern**: Decouples monitoring from core logic
4. **Value Objects**: Ensures thread safety through immutability
5. **Heartbeat**: Detects failures without blocking server
6. **Thread Pool**: Scales to handle many clients efficiently
7. **Synchronized Methods**: Prevents race conditions on critical operations

### Best Practices Demonstrated

- ✅ Separation of concerns (each class has one job)
- ✅ DRY principle (no code duplication)
- ✅ SOLID principles (especially Open/Closed and Single Responsibility)
- ✅ Defensive programming (validate all inputs)
- ✅ Fail-fast (detect errors early)
- ✅ Graceful degradation (system continues on failure)
- ✅ Comprehensive testing (test all critical paths)

