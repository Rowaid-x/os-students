
import java.util.*;

public class Room {
    private final String name;
    private final List<User> users = Collections.synchronizedList(new ArrayList<>());
    private User moderator;

    public Room(String name, User creator) {
        this.name = name;
        this.moderator = creator;
        users.add(creator);
    }

    public String getName() {
        return name;
    }

    public synchronized void addUser(User user) {
        users.add(user);
        broadcast(null, user.getPseudonym() + " has joined the room.");
    }

    public synchronized boolean removeUser(User user) {
        if (users.remove(user)) {
            broadcast(null, user.getPseudonym() + " has left the room.");
            if (user.equals(moderator) && !users.isEmpty()) {
                moderator = users.get(0);
                broadcast(null, moderator.getPseudonym() + " is now the moderator.");
            }
            return true;
        }
        return false;
    }

    public synchronized void kickUser(User user, String reason) {
        if (users.contains(user)) {
            users.remove(user);
            user.sendMessage("You have been removed from the room: " + name + ". Reason: " + reason);
            broadcast(null, user.getPseudonym() + " has been kicked from the room. Reason: " + reason);
        }
    }

    public synchronized void broadcast(User sender, String message) {
        String prefix = sender == null ? "[Room Notice]" : sender.getPseudonym() + ":";
        for (User user : users) {
            user.sendMessage(prefix + " " + message);
        }
    }

    public synchronized boolean isEmpty() {
        return users.isEmpty();
    }

    public synchronized boolean isModerator(User user) {
        return user.equals(moderator);
    }
}
