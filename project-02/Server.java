
import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 13337; // Server port
    static final List<User> users = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Room> rooms = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, String> tickets = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        System.out.println("Starting server on port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                User user = new User(clientSocket);
                users.add(user);
                new Thread(user).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static synchronized String generateTicket(String pseudonym) {
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, pseudonym);
        return ticket;
    }

    public static synchronized boolean validateTicket(String ticket) {
        return tickets.containsKey(ticket);
    }

    public static synchronized String getPseudonymByTicket(String ticket) {
        return tickets.get(ticket);
    }

    public static synchronized void addRoom(String roomName, Room room) {
        rooms.put(roomName, room);
    }

    public static synchronized Room getRoom(String roomName) {
        return rooms.get(roomName);
    }

    public static synchronized void removeRoom(String roomName) {
        rooms.remove(roomName);
    }
}
