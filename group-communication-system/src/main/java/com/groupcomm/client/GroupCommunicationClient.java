package com.groupcomm.client;

import com.groupcomm.shared.Message;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class GroupCommunicationClient extends JFrame {
    
    // Components
    private JTextField serverIpField;
    private JTextField serverPortField;
    private JTextField memberIdField;
    private JButton connectButton;
    private JButton disconnectButton;
    
    private JTextArea messageArea; // Unified display
    private JTextField messageField;
    private JButton sendButton;
    
    private JLabel statusLabel;
    private JLabel coordinatorLabel;
    
    // Network
    private Socket socket;
    private PrintWriter output;
    private Scanner input;
    private Thread receiverThread;
    
    private String memberId;
    private boolean connected;
    private boolean isCoordinator;
    
    public GroupCommunicationClient() {
        super("Group Communication Client");
        this.connected = false;
        
        initializeGUI();
        setupEventHandlers();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);
    }
    
    private void initializeGUI() {
        setLayout(new BorderLayout(5, 5));
        
        // 1. Top Panel (Connection)
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        
        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connPanel.add(new JLabel("IP:"));
        serverIpField = new JTextField("127.0.0.1", 10);
        connPanel.add(serverIpField);
        connPanel.add(new JLabel("Port:"));
        serverPortField = new JTextField("8888", 5);
        connPanel.add(serverPortField);
        connPanel.add(new JLabel("ID:"));
        memberIdField = new JTextField(8);
        connPanel.add(memberIdField);
        
        connectButton = new JButton("Connect");
        connPanel.add(connectButton);
        disconnectButton = new JButton("Disconnect");
        disconnectButton.setEnabled(false);
        connPanel.add(disconnectButton);
        
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Status: Disconnected");
        statusLabel.setForeground(Color.RED);
        coordinatorLabel = new JLabel(" | Coordinator: N/A");
        statusPanel.add(statusLabel);
        statusPanel.add(coordinatorLabel);
        
        topPanel.add(connPanel);
        topPanel.add(statusPanel);
        add(topPanel, BorderLayout.NORTH);
        
        // 2. Center Panel (Messages)
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        add(new JScrollPane(messageArea), BorderLayout.CENTER);
        
        // 3. Bottom Panel (Input)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.setEnabled(false);
        sendButton = new JButton("Send");
        sendButton.setEnabled(false);
        
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }
    
    private void connect() {
        String ip = serverIpField.getText().trim();
        String id = memberIdField.getText().trim();
        int port = 8888;
        
        try { port = Integer.parseInt(serverPortField.getText().trim()); } catch(Exception e) {}
        
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter ID");
            return;
        }
        
        try {
            socket = new Socket(ip, port);
            input = new Scanner(socket.getInputStream());
            output = new PrintWriter(socket.getOutputStream(), true);
            
            // Handshake
            if (input.hasNextLine() && input.nextLine().equals("SUBMITID")) {
                output.println(id);
                String response = input.nextLine();
                
                if (response.startsWith("ACCEPTED")) {
                    String[] parts = response.split("\\|");
                    this.memberId = parts[1];
                    this.isCoordinator = Boolean.parseBoolean(parts[2]);
                    
                    connected = true;
                    updateGUIConnected();
                    
                    // Start Listener
                    receiverThread = new Thread(this::listenForMessages);
                    receiverThread.setDaemon(true);
                    receiverThread.start();
                } else {
                    JOptionPane.showMessageDialog(this, response);
                    socket.close();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection Failed: " + e.getMessage());
        }
    }
    
    private void listenForMessages() {
        try {
            while (connected && input.hasNextLine()) {
                String line = input.nextLine();
                processMessage(line);
            }
        } catch (Exception e) {
            if (connected) appendMsg("Connection Lost.");
        } finally {
            disconnect();
        }
    }
    
    private void processMessage(String line) {
        try {
            Message msg = Message.fromProtocolString(line);
            switch (msg.getType()) {
                case PING:
                    output.println(new Message(memberId, null, "PONG", Message.MessageType.PONG).toProtocolString());
                    break;
                case SYSTEM:
                    handleSystemMsg(msg);
                    break;
                default:
                    appendMsg(msg.toString());
            }
        } catch (Exception e) {
            appendMsg(line); // Fallback
        }
    }
    
    private void handleSystemMsg(Message msg) {
        String content = msg.getContent();
        
        // Update Coordinator Label if needed
        if (content.contains("YOU ARE THE COORDINATOR") || content.contains("is the new COORDINATOR")) {
            String labelText = content.contains("YOU") ? "YOU" : "Changed"; 
            // Simple logic: if message says "YOU", update label. 
            // If it says someone else is new coord, we could parse it, 
            // but the message text itself is sufficient notification.
            if(content.contains("YOU")) {
                SwingUtilities.invokeLater(() -> coordinatorLabel.setText(" | Coordinator: YOU"));
            }
        }
        
        // Also display the message in chat
        appendMsg("[SYSTEM] " + content);
    }
    
    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            output.println(text); // Server will echo this back now
            messageField.setText("");
        }
    }
    
    private void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            messageField.setEnabled(false);
            sendButton.setEnabled(false);
            statusLabel.setText("Status: Disconnected");
        });
    }
    
    private void updateGUIConnected() {
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            messageField.setEnabled(true);
            sendButton.setEnabled(true);
            statusLabel.setText("Status: Connected as " + memberId);
            if (isCoordinator) coordinatorLabel.setText(" | Coordinator: YOU");
        });
    }
    
    private void appendMsg(String text) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(text + "\n");
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GroupCommunicationClient().setVisible(true));
    }
}