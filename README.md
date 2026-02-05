group-communication-system/
├── src/
│   ├── main/java/com/groupcomm/
│   │   ├── client/
│   │   │   └── GroupCommunicationClient.java (GUI client)
│   │   ├── server/
│   │   │   ├── GroupCommunicationServer.java (main server)
│   │   │   ├── GroupRegistry.java (singleton registry)
│   │   │   ├── ClientHandler.java (per-client handler)
│   │   │   └── HeartbeatMonitor.java (health monitor)
│   │   ├── shared/
│   │   │   ├── Message.java (value object)
│   │   │   └── MemberInfo.java (value object)
│   │   └── patterns/
│   │       ├── MessageStrategy.java (strategy interface)
│   │       ├── BroadcastMessageStrategy.java (concrete)
│   │       └── PrivateMessageStrategy.java (concrete)
│   └── test/java/com/groupcomm/
│       └── GroupCommunicationSystemTest.java (12 tests)
├── README.md (comprehensive documentation)
├── IMPLEMENTATION_GUIDE.md (detailed implementation)
├── MARKING_GUIDE.md (requirements mapping)
├── QUICK_START.md (getting started guide)
├── PROJECT_SUMMARY.md (this file)
├── build.sh (compilation script)
├── run-server.sh (server launcher)
└── run-client.sh (client launcher)
