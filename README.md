<img width="505" height="512" alt="image" src="https://github.com/user-attachments/assets/f38fc3b8-a386-4d21-aef8-19c98cbf8a5b" />


Milestone 1: Dynamic Connection & Identity The Task: Allow users to input an ID, IP, and Port via GUI.

Logic: Modify ChatClient to use a dialog box or a startup panel for these parameters.

Requirement Met: "A client may provide ID, port, and IP as input."

Milestone 2: The Coordinator Election logic The Task: Automatically assign a leader.

Logic: 1. In ChatServer, when a client connects, check if the names set is empty. 2. If empty, flag that client as isCoordinator = true. 3. Broadcast a message to all: "User [ID] is the coordinator."

Requirement Met: "First connection becomes coordinator... whoever joins must be informed."

Milestone 3: Health Monitoring (Fault Tolerance) The Task: The coordinator must maintain the state and detect leaves.

Logic: 1. Implement a ScheduledExecutorService in the server to send a PING command to all clients every 20 seconds. 2. If a PrintWriter throws an exception during the ping, the server assumes that member left.

Requirement Met: "Coordinator maintains state... by a periodic ping."

Milestone 4: Advanced Messaging (Private & Broadcast) The Task: Send messages to specific people or everyone.

Logic:

In the ChatServer handler, parse the input string.

If it starts with @ID, look up that specific PrintWriter in a Map (e.g., HashMap<String, PrintWriter>) and send only to them.

Otherwise, loop through all writers for a broadcast.

Requirement Met: "Anyone can send private or broadcast messages."

Milestone 5: The "New Coordinator" Election The Task: If the leader leaves, the system must not crash.

Logic:

In the finally block of ChatServer.Handler, check if the name leaving was the current coordinator.

If yes, pick the next name in the HashSet and notify the group.

Requirement Met: "If the coordinator leaves, then any existing member will become a coordinator."
