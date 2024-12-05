
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class User implements Runnable {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private String pseudonym;
    private String ticket;
    private Room currentRoom;

    public User(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void run() {
        try {
            out.println("Welcome! Identify yourself with 'pseudo [name]' or 'ticket [ticket]'.");
            String line;

            while ((line = in.readLine()) != null) {
                handleCommand(line.trim());
            }
        } catch (IOException e) {
            System.err.println("Error handling user: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleCommand(String commandLine) {
        String[] parts = commandLine.split(" ", 2);
        String command = parts[0].toLowerCase();

        try {
            switch (command) {
                case "pseudo":
                    handlePseudo(parts.length > 1 ? parts[1] : null);
                    break;

                case "ticket":
                    handleTicket(parts.length > 1 ? parts[1] : null);
                    break;

                case "join":
                    handleJoin(parts.length > 1 ? parts[1] : null);
                    break;

                case "leave":
                    handleLeave();
                    break;

                case "send":
                    handleSend(parts.length > 1 ? parts[1] : null);
                    break;

                case "direct":
                    handleDirect(parts.length > 1 ? parts[1] : null);
                    break;

                default:
                    sendMessage("Error: Unrecognized command.");
            }
        } catch (Exception e) {
            sendMessage("Error: " + e.getMessage());
        }
    }

    private void handlePseudo(String pseudonym) {
        if (pseudonym == null || pseudonym.isEmpty()) {
            sendMessage("Error: Provide a pseudonym.");
            return;
        }

        this.pseudonym = pseudonym;
        this.ticket = generateTicket(pseudonym);
        sendMessage("Welcome, " + pseudonym + "! Your ticket is " + ticket);
    }

    private static String generateTicket(String seq) {
        byte[] hash = String.format("%32s", seq).getBytes();
        try {
            for (int i = 0; i < Math.random() * 64 + 1; ++i) {
                hash = MessageDigest.getInstance("SHA-256").digest(hash);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return HexFormat.ofDelimiter(":").formatHex(hash).toString().substring(78);
    }

    private void handleTicket(String ticket) {
        if (ticket == null || !Server.validateTicket(ticket)) {
            sendMessage("Error: Invalid ticket.");
            return;
        }

        this.ticket = ticket;
        this.pseudonym = Server.getPseudonymByTicket(ticket);
        sendMessage("Welcome back, " + pseudonym + "!");
    }

    private void handleJoin(String roomName) {
        if (roomName == null || roomName.isEmpty()) {
            sendMessage("Error: Provide a room name.");
            return;
        }

        if (currentRoom != null) {
            sendMessage("Error: You are already in a room. Leave it before joining another.");
            return;
        }

        Room room = Server.getRoom(roomName);
        if (room == null) {
            room = new Room(roomName, this);
            Server.addRoom(roomName, room);
            sendMessage("Created and joined room: " + roomName);
        } else {
            room.addUser(this);
            sendMessage("Joined room: " + roomName);
        }
        this.currentRoom = room;
    }

    private void handleLeave() {
        if (currentRoom == null) {
            sendMessage("Error: You are not in a room.");
            return;
        }

        currentRoom.removeUser(this);
        if (currentRoom.isEmpty()) {
            Server.removeRoom(currentRoom.getName());
        }
        this.currentRoom = null;
    }

    private void handleSend(String message) {
        if (currentRoom == null) {
            sendMessage("Error: You are not in a room.");
            return;
        }

        if (message == null || message.isEmpty()) {
            sendMessage("Error: Provide a message to send.");
            return;
        }

        currentRoom.broadcast(this, message);
    }

    private void handleDirect(String data) {
        if (data == null || !data.contains(" ")) {
            sendMessage("Error: Provide a user and a message.");
            return;
        }

        String[] parts = data.split(" ", 2);
        String targetUser = parts[0];
        String message = parts[1];

        for (User user : Server.users) {
            if (user.getPseudonym().equalsIgnoreCase(targetUser)) {
                user.sendMessage("Direct message from " + pseudonym + ": " + message);
                sendMessage("Message sent to " + targetUser + ".");
                return;
            }
        }

        sendMessage("Error: User not found.");
    }

    private void disconnect() {
        if (currentRoom != null) {
            currentRoom.removeUser(this);
            if (currentRoom.isEmpty()) {
                Server.removeRoom(currentRoom.getName());
            }
        }

        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getPseudonym() {
        return pseudonym;
    }
}
