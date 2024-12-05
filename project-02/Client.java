
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Client {
    private static String ticket = null; // Stores the user's ticket for authentication
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"; // Timestamp format

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java Client <server_ip> <port>");
            return;
        }

        String serverIp = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket socket = new Socket(serverIp, port)) {
            System.out.println("Connected to server at " + serverIp + ":" + port);

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            // Start a thread to handle server messages
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println("[Server] " + timestamp() + ": " + serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println("Server connection lost.");
                }
            }).start();

            // Main loop for sending commands to the server
            while (true) {
                System.out.print("> ");
                String command = console.readLine();
                if (command == null || command.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting...");
                    break;
                }

                if (command.startsWith("ident")) {
                    handleIdentification(out, console);
                } else if (command.startsWith("ticket")) {
                    saveTicket(command.split(" ")[1]);
                } else {
                    out.println(command);
                }
            }

        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }

    private static void handleIdentification(PrintWriter out, BufferedReader console) throws IOException {
        if (ticket != null) {
            out.println("ticket " + ticket);
        } else {
            System.out.print("Enter your pseudonym: ");
            String pseudonym = console.readLine();
            out.println("pseudo " + pseudonym);
        }
    }

    private static void saveTicket(String newTicket) {
        try (FileWriter writer = new FileWriter("ticket.txt")) {
            writer.write(newTicket);
            ticket = newTicket;
            System.out.println("Ticket saved locally.");
        } catch (IOException e) {
            System.err.println("Error saving ticket: " + e.getMessage());
        }
    }

    private static void loadTicket() {
        try (BufferedReader reader = new BufferedReader(new FileReader("ticket.txt"))) {
            ticket = reader.readLine();
            System.out.println("Loaded ticket from file.");
        } catch (IOException e) {
            System.out.println("No saved ticket found. Please identify yourself.");
        }
    }

    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT));
    }
}
