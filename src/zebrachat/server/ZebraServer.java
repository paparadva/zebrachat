package zebrachat.server;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import zebrachat.protocol.*;

public class ZebraServer {

    private static final int PORT = Config.getPort();
    private static final int RETAIN_MESSAGES = Config.getRetainedMessagesNumber();
    private static final int MAXIMUM_CONNECTIONS = Config.getMaximumConnections();
    static final Logger log = Logger.getLogger("zebrachat.server");

    private final Map<String, Connection> connections = new HashMap<>();
    //private final Set<zebrachat.server.Connection> connections = new HashSet<>();
    private final Deque<ChatMessage> lastMessages = new ConcurrentLinkedDeque<>();

    private void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Running server at port " + PORT);
            while (true) {
                Connection connection = initializeConnection(serverSocket.accept());
                if(connection == null) {
                    continue;
                }

                if(!verifyLogin(connection)) {
                    connection.close();
                    continue;
                }

                if(!joinChat(connection)) {
                    connection.close();
                    continue;
                }

                if(!forwardMessages(connection)) {
                    connection.close();
                    continue;
                }

                advertiseNewUser(connection);

                log.info("Starting connection thread");
                connection.start();
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Unexpected server exception", e);

        } finally {
            for(Connection conn: connections.values()) {
                conn.close();
            }
        }
    }

    private void advertiseNewUser(Connection connection) {
        ChatMessage greeting = new ChatMessage(null,
                connection.getUsername() + " has joined the chat!");
        greeting.setReceiptTime(LocalDateTime.now());
        broadcast(greeting);
    }

    private boolean forwardMessages(Connection connection) {
        synchronized (lastMessages) {
            boolean success = false;
            log.info("Forwarding last messages");
            try {
                for (ChatMessage msg : lastMessages) {
                    connection.sendMessage(msg);
                }
                success = true;

            } catch (IOException e) {
                log.log(Level.WARNING, "Error while forwarding messages", e);
                return false;
            }

            return success;
        }
    }

    private boolean joinChat(Connection connection) {
        boolean success = false;
        try {
            if(connections.size() < MAXIMUM_CONNECTIONS) {
                SystemMessage message = new SystemMessage(SystemMessage.Status.OK, "permission granted");
                connection.sendMessage(message);
                connections.put(connection.getUsername(), connection);
                success = true;

            } else {
                SystemMessage message = new SystemMessage(SystemMessage.Status.ERROR,
                        "maximum number of clients reached");
                connection.sendMessage(message);
                log.info("No more chat slots - closing client connection");
                success= false;
            }

        } catch (IOException e) {
            log.log(Level.WARNING, "Error while joining chat", e);
            return false;
        }

        return success;
    }

    private boolean verifyLogin(Connection connection) {
        log.info("Verifying login");
        try {
            LoginRequest login = connection.readLoginRequest();
            String username = login.getUsername();
            String password = login.getPassword();
            String correctpw = Config.users.get(username);

            if(connections.containsKey(username)) {
                connection.sendMessage(new SystemMessage(SystemMessage.Status.ERROR, "user is already logged in"));
                return false;
            } else if(!password.equals(correctpw) || !Config.users.containsKey(username)) {
                connection.sendMessage(new SystemMessage(SystemMessage.Status.ERROR, "wrong login"));
                return false;
            } else {
                connection.sendMessage(new SystemMessage(SystemMessage.Status.OK, "login successful"));
                return true;
            }

        } catch (IOException | ClassNotFoundException e) {
            log.log(Level.WARNING, "Error reading login", e);
            return false;
        }
    }

    private Connection initializeConnection(Socket socket) {
        log.info("New client connection requested: " + socket.getInetAddress());
        Connection connection;
        try {
            connection = new Connection(socket, this);

        } catch (Exception e) {
            log.warning("" + socket.getInetAddress() + " failed to initialize connection");
            return null;
        }

        return connection;
    }

    public void processUserLeft(String username) {
        synchronized (connections) {
            if(connections.containsKey(username)) {
                log.info("Logging out " + username);
                ChatMessage userLeft = new ChatMessage(null, username + " has left.");
                userLeft.setReceiptTime(LocalDateTime.now());
                broadcast(userLeft);
                connections.remove(username);
            }
        }
    }

    synchronized public void broadcast(ChatMessage message) {
        log.info("Broadcasting message: " + message);

        lastMessages.addLast(message);
        if (lastMessages.size() > RETAIN_MESSAGES) {
            lastMessages.removeFirst();
        }

        for (Connection connection : connections.values()) {
            try {
                connection.sendMessage(message);
            } catch (IOException e) {
                log.warning("Error while sending message: " + message + "\n " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new ZebraServer().run();
    }
}
