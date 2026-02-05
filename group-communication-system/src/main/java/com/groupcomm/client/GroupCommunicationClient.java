package com.groupcomm.client;

import com.groupcomm.shared.Message;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * GUI-based client for the group communication system.
 * 
 * Design Pattern: Observer Pattern (updates GUI based on server messages)
 * Features:
 * - Dynamic connection with user-specified ID, IP, and port
 * - Real-time message display with timestamps
 * - Support for broadcast and private messaging
 * - Coordinator status display
 * - Thread-safe GUI updates using SwingUtilities
 */
public class GroupCommunicationClient extends JFrame {
    
    // GUI Components
    private JTextField serverIpField;
    private JTextField serverPortField;
    private JTextField memberIdField;
    private JButton connectButton;
    private JButton disconnectButton;
    
    private JTextArea messageArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton whoButton;
    private JButton helpButton;
    
    private JLabel statusLabel;
    private JLabel coordinatorLabel;
    
    // Network components
    private Socket socket;
    private PrintWriter output;
    private Scanner input;
    private Thread receiverThread;
    
    // State
    private String memberId;
    private boolean connected;
    private boolean isCoordinator;
    
    /**
     * Constructs the client GUI.
     */
    public GroupCommunicationClient() {
        super("Group Communication Client");
        this.connected = false;
        this.isCoordinator = false;
        
        initializeGUI();
        setupEventHandlers();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
    }
    
    /**
     * Initializes all GUI components.
     */
    private void initializeGUI() {
        setLayout(new BorderLayout(10, 10));
        
        // Connection Panel (Top)
        JPanel connectionPanel = createConnectionPanel();
        add(connectionPanel, BorderLayout.NORTH);
        
        // Message Display Area (Center)
        JPanel messagePanel = createMessagePanel();
        add(messagePanel, BorderLayout.CENTER);
        
        // Input Panel (Bottom)
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.SOUTH);
        
        // Status Panel (Very Bottom)
        JPanel statusPanel = createStatusPanel();
        add(statusPanel, BorderLayout.PAGE_END);
    }
    
    /**
     * Creates the connection configuration panel.
     */
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Connection"));
        
        // Server IP
        panel.add(new JLabel("Server IP:"));
        serverIpField = new JTextField("127.0.0.1", 12);
        panel.add(serverIpField);
        
        // Server Port
        panel.add(new JLabel("Port:"));
        serverPortField = new JTextField("8888", 6);
        panel.add(serverPortField);
        
        // Member ID
        panel.add(new JLabel("Your ID:"));
        memberIdField = new JTextField(10);
        panel.add(memberIdField);
        
        // Connect/Disconnect Buttons
        connectButton = new JButton("Connect");
        connectButton.setBackground(new Color(76, 175, 80));
        connectButton.setForeground(Color.WHITE);
        panel.add(connectButton);
        
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        disconnectButton.setBackground(new Color(244, 67, 54));
        disconnectButton.setForeground(Color.WHITE);
        panel.add(disconnectButton);
        
        return panel;
    }
    
    /**
     * Creates the message display panel.
     */
    private JPanel createMessagePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Messages"));
        
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Creates the message input panel.
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Message input field
        messageField = new JTextField();
        messageField.setEnabled(false);
        messageField.setFont(new Font("SansSerif", Font.PLAIN, 12));
        panel.add(messageField, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        sendButton.setBackground(new Color(33, 150, 243));
        sendButton.setForeground(Color.WHITE);
        buttonPanel.add(sendButton);
        
        whoButton = new JButton("Who's Online");
        whoButton.setEnabled(false);
        buttonPanel.add(whoButton);
        
        helpButton = new JButton("Help");
        helpButton.setEnabled(false);
        buttonPanel.add(helpButton);
        
        panel.add(buttonPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Creates the status display panel.
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        panel.add(statusLabel);
        
        coordinatorLabel = new JLabel("Coordinator: N/A");
        panel.add(coordinatorLabel);
        
        return panel;
    }
    
    /**
     * Sets up event handlers for all interactive components.
     * Implements the Observer pattern by responding to user actions.
     */
    private void setupEventHandlers() {
        // Connect button
        connectButton.addActionListener(e -> connectToServer());
        
        // Disconnect button
        disconnectButton.addActionListener(e -> disconnectFromServer());
        
        // Send button
        sendButton.addActionListener(e -> sendMessage());
        
        // Message field - send on Enter
        messageField.addActionListener(e -> sendMessage());
        
        // Who button
        whoButton.addActionListener(e -> sendCommand("/who"));
        
        // Help button
        helpButton.addActionListener(e -> sendCommand("/help"));
        
        // Window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connected) {
                    disconnectFromServer();
                }
            }
        });
    }
    
    /**
     * Connects to the server.
     * Milestone 1: Dynamic Connection & Identity
     */
    private void connectToServer() {
        String serverIp = serverIpField.getText().trim();
        String portText = serverPortField.getText().trim();
        String proposedId = memberIdField.getText().trim();
        
        // Validation
        if (serverIp.isEmpty()) {
            showError("Please enter server IP address");
            return;
        }
        
        if (proposedId.isEmpty()) {
            showError("Please enter your member ID");
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                showError("Port must be between 1 and 65535");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid port number");
            return;
        }
        
        // Attempt connection
        try {
            appendMessage("=== Connecting to " + serverIp + ":" + port + " ===");
            
            socket = new Socket(serverIp, port);
            input = new Scanner(socket.getInputStream());
            output = new PrintWriter(socket.getOutputStream(), true);
            
            // Wait for SUBMITID request
            if (!input.hasNextLine()) {
                throw new IOException("Server closed connection");
            }
            
            String response = input.nextLine();
            if (!response.equals("SUBMITID")) {
                throw new IOException("Unexpected server response: " + response);
            }
            
            // Send member ID
            output.println(proposedId);
            output.flush();
            
            // Wait for acceptance or error
            if (!input.hasNextLine()) {
                throw new IOException("Server closed connection");
            }
            
            response = input.nextLine();
            
            if (response.startsWith("ERROR")) {
                String errorMsg = response.substring(6); // Remove "ERROR|"
                showError("Connection rejected: " + errorMsg);
                socket.close();
                return;
            }
            
            if (!response.startsWith("ACCEPTED")) {
                throw new IOException("Unexpected server response: " + response);
            }
            
            // Parse acceptance message: ACCEPTED|memberID|isCoordinator
            String[] parts = response.split("\\|");
            if (parts.length >= 3) {
                this.memberId = parts[1];
                this.isCoordinator = Boolean.parseBoolean(parts[2]);
            } else {
                this.memberId = proposedId;
            }
            
            // Connection successful
            connected = true;
            
            // Update GUI
            SwingUtilities.invokeLater(() -> {
                connectButton.setEnabled(false);
                disconnectButton.setEnabled(true);
                serverIpField.setEnabled(false);
                serverPortField.setEnabled(false);
                memberIdField.setEnabled(false);
                
                // FIXED: Enable message input controls
                messageField.setEnabled(true);
                sendButton.setEnabled(true);
                whoButton.setEnabled(true);
                helpButton.setEnabled(true);
                
                statusLabel.setText("Status: Connected as " + memberId);
                statusLabel.setForeground(new Color(76, 175, 80));
                
                // FIXED: Update coordinator label immediately
                if (isCoordinator) {
                    coordinatorLabel.setText("Coordinator: YOU");
                    coordinatorLabel.setForeground(new Color(255, 152, 0));
                }
                
                appendMessage("=== Connected successfully as " + memberId + " ===");
                if (isCoordinator) {
                    appendMessage("=== You are the COORDINATOR ===");
                }
                
                // Focus message field for immediate typing
                messageField.requestFocusInWindow();
            });
            
            // Start receiver thread
            receiverThread = new Thread(this::receiveMessages);
            receiverThread.setDaemon(true);
            receiverThread.start();
            
        } catch (IOException e) {
            showError("Connection failed: " + e.getMessage());
            cleanup();
        }
    }
    
    /**
     * Disconnects from the server.
     */
    private void disconnectFromServer() {
        if (!connected) {
            return;
        }
        
        try {
            if (output != null) {
                output.println("/quit");
                output.flush();
            }
        } catch (Exception e) {
            // Ignore
        }
        
        cleanup();
        updateGUIAfterDisconnect();
    }
    
    /**
     * Cleans up network resources.
     */
    private void cleanup() {
        connected = false;
        
        try {
            if (receiverThread != null && receiverThread.isAlive()) {
                receiverThread.interrupt();
            }
            if (input != null) {
                input.close();
            }
            if (output != null) {
                output.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
        
        socket = null;
        input = null;
        output = null;
        receiverThread = null;
        memberId = null;
        isCoordinator = false;
    }
    
    /**
     * Updates GUI after disconnection.
     */
    private void updateGUIAfterDisconnect() {
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            serverIpField.setEnabled(true);
            serverPortField.setEnabled(true);
            memberIdField.setEnabled(true);
            
            messageField.setEnabled(false);
            messageField.setText("");
            sendButton.setEnabled(false);
            whoButton.setEnabled(false);
            helpButton.setEnabled(false);
            
            statusLabel.setText("Status: Disconnected");
            statusLabel.setForeground(Color.RED);
            coordinatorLabel.setText("Coordinator: N/A");
            coordinatorLabel.setForeground(Color.BLACK);
            
            appendMessage("=== Disconnected from server ===");
        });
    }
    
    /**
     * Sends a message to the server.
     * Supports private messages with @username syntax.
     */
    private void sendMessage() {
        if (!connected) {
            return;
        }
        
        String text = messageField.getText().trim();
        
        if (text.isEmpty()) {
            return;
        }
        
        try {
            output.println(text);
            output.flush();
            
            messageField.setText("");
            
        } catch (Exception e) {
            appendMessage("Error sending message: " + e.getMessage());
        }
    }
    
    /**
     * Sends a command to the server.
     * 
     * @param command The command to send
     */
    private void sendCommand(String command) {
        if (!connected) {
            return;
        }
        
        try {
            output.println(command);
            output.flush();
        } catch (Exception e) {
            appendMessage("Error sending command: " + e.getMessage());
        }
    }
    
    /**
     * Receives messages from the server.
     * Runs in a separate thread to avoid blocking the GUI.
     * Uses SwingUtilities.invokeLater for thread-safe GUI updates.
     */
    private void receiveMessages() {
        try {
            while (connected && input.hasNextLine()) {
                String line = input.nextLine();
                
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                
                processServerMessage(line);
            }
        } catch (Exception e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    appendMessage("Connection lost: " + e.getMessage());
                    disconnectFromServer();
                });
            }
        }
    }
    
    /**
     * Processes a message from the server.
     * Handles different message types appropriately.
     * 
     * @param line The message line
     */
    private void processServerMessage(String line) {
        try {
            Message message = Message.fromProtocolString(line);
            
            // Handle different message types
            switch (message.getType()) {
                case PING:
                    // Respond to ping
                    output.println(new Message(memberId, null, "PONG", Message.MessageType.PONG).toProtocolString());
                    output.flush();
                    break;
                    
                case SYSTEM:
                    handleSystemMessage(message);
                    break;
                    
                case BROADCAST:
                case PRIVATE:
                    displayMessage(message);
                    break;
                    
                default:
                    displayMessage(message);
            }
            
        } catch (Exception e) {
            // If parsing fails, display as plain text
            SwingUtilities.invokeLater(() -> appendMessage(line));
        }
    }
    
    /**
     * Handles system messages.
     * Updates coordinator information if needed.
     * 
     * @param message The system message
     */
    private void handleSystemMessage(Message message) {
        String content = message.getContent();
        
        // FIXED: Detect if this user is the coordinator
        if (content.contains("You are the COORDINATOR")) {
            SwingUtilities.invokeLater(() -> {
                isCoordinator = true;
                coordinatorLabel.setText("Coordinator: YOU");
                coordinatorLabel.setForeground(new Color(255, 152, 0));
            });
        }
        
        // FIXED: Parse current coordinator information
        if (content.contains("Current Coordinator is:")) {
            SwingUtilities.invokeLater(() -> {
                try {
                    // Format: "Connected. Current Coordinator is: ID (IP:Port) [COORDINATOR]"
                    int startIdx = content.indexOf("is: ") + 4;
                    if (startIdx > 3) {
                        String coordInfo = content.substring(startIdx).trim();
                        // Extract just the ID (everything before the first space or parenthesis)
                        int endIdx = coordInfo.indexOf(' ');
                        if (endIdx < 0) {
                            endIdx = coordInfo.indexOf('(');
                        }
                        if (endIdx > 0) {
                            String coordId = coordInfo.substring(0, endIdx).trim();
                            coordinatorLabel.setText("Coordinator: " + coordId);
                            coordinatorLabel.setForeground(Color.BLACK);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing coordinator info: " + e.getMessage());
                }
            });
        }
        
        // FIXED: Handle coordinator changes
        if (content.contains("is the new COORDINATOR")) {
            try {
                String newCoordinator = content.split(" ")[0].trim();
                
                SwingUtilities.invokeLater(() -> {
                    if (newCoordinator.equals(memberId)) {
                        isCoordinator = true;
                        coordinatorLabel.setText("Coordinator: YOU");
                        coordinatorLabel.setForeground(new Color(255, 152, 0));
                    } else {
                        isCoordinator = false;
                        coordinatorLabel.setText("Coordinator: " + newCoordinator);
                        coordinatorLabel.setForeground(Color.BLACK);
                    }
                });
            } catch (Exception e) {
                System.err.println("Error parsing coordinator change: " + e.getMessage());
            }
        }
        
        // FIXED: Handle "You are now the COORDINATOR" message
        if (content.contains("You are now the COORDINATOR")) {
            SwingUtilities.invokeLater(() -> {
                isCoordinator = true;
                coordinatorLabel.setText("Coordinator: YOU");
                coordinatorLabel.setForeground(new Color(255, 152, 0));
            });
        }
        
        displayMessage(message);
    }
    
    /**
     * Displays a message in the message area.
     * Thread-safe GUI update.
     * 
     * @param message The message to display
     */
    private void displayMessage(Message message) {
        SwingUtilities.invokeLater(() -> {
            String formatted;
            
            if (message.getType() == Message.MessageType.SYSTEM) {
                formatted = "[" + message.getFormattedTimestamp() + "] *** " + message.getContent() + " ***";
            } else if (message.getType() == Message.MessageType.PRIVATE) {
                formatted = "[" + message.getFormattedTimestamp() + "] [PRIVATE] " + 
                           message.getSenderId() + ": " + message.getContent();
            } else {
                formatted = message.toString();
            }
            
            messageArea.append(formatted + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    /**
     * Appends a plain text message to the display.
     * 
     * @param text The text to append
     */
    private void appendMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(text + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    /**
     * Shows an error dialog.
     * 
     * @param message The error message
     */
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> 
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }
    
    /**
     * Main method to start the client application.
     * 
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default look and feel
        }
        
        // Create and show GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            GroupCommunicationClient client = new GroupCommunicationClient();
            client.setVisible(true);
        });
    }
}