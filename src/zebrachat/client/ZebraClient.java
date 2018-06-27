package zebrachat.client;

import zebrachat.protocol.ChatMessage;
import zebrachat.protocol.LoginRequest;
import zebrachat.protocol.SystemMessage;
import zebrachat.protocol.UserStatus;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ZebraClient implements Closeable {
    private static int SERVER_PORT = 1234;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private BufferedReader stdIn;
    private static final Logger log = Logger.getLogger("zebrachat.client");

    private String username;
    private String password;
    private UserStatus userStatus = UserStatus.NOTHING;

    public ZebraClient(String hostName, String username, String password) throws Exception{
        this.username = username;
        this.password = password;

        try {
            socket = new Socket(hostName, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            stdIn = new BufferedReader(new InputStreamReader(System.in));

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while establishing connection", e);
            close();
            throw e;
        }
    }

    public void run() {
        try {
            if (!requestLogin()) {
                System.out.println("Wrong login");
                System.exit(0);
            }
            if (!receivePermission()) {
                System.out.println("Connection permission denied");
                System.exit(0);
            }
            Thread msgThread = listenForMessagesThread((ChatMessage msg) -> {
                if (msg.getUsername() != null) {
                    System.out.println(msg.getUsername() + " [" + msg.getStatus() + "]: " + msg.getText());
                } else {
                    System.out.println(msg.getText());
                }
            });
            Thread inputThread = processUserInput();

            msgThread.start();
            inputThread.start();

            msgThread.join();
            inputThread.join();
        } catch (InterruptedException e) {
            log.log(Level.SEVERE, "InterruptedException", e);
        } finally {
            close();
        }
    }

    public void setUserStatus(UserStatus userStatus) {
        this.userStatus = userStatus;
    }

    public boolean receivePermission() {
        SystemMessage msg = null;
        try {
            msg = (SystemMessage) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        System.out.println(msg.getMessage());
        return msg.getStatus() == SystemMessage.Status.OK;
    }

    public boolean requestLogin() {
        SystemMessage response = null;
        try {
            LoginRequest login = new LoginRequest(username, password);
            out.writeObject(login);
            response = (SystemMessage) in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return response.getStatus() == SystemMessage.Status.OK;
    }

    private Thread listenForMessagesThread(NewMessageCallback callback) {
        Thread t = new Thread(() -> {
            try {
                listenForMessages(callback);

            } catch (IOException e) {
                log.log(Level.SEVERE, "Error while listening to messages", e);

            } finally {
                close();
            }
        });

        return t;
    }

    public void listenForMessages(NewMessageCallback callback) throws IOException {
        ChatMessage fromServer;
        while (true) {
            try {
                fromServer = (ChatMessage) in.readObject();
            } catch (ClassNotFoundException e) {
                System.err.println("Error deseriazlizing message");
                e.printStackTrace();
                continue;
            }

            callback.processChatMessage(fromServer);
        }
    }

    public void sendChatMessage(String text) throws IOException {
        ChatMessage message = new ChatMessage(username, text);
        out.writeObject(message);
    }

    private Thread processUserInput() {
        Thread t = new Thread(() -> {
            try {
                String messageText;
                while ((messageText = stdIn.readLine()) != null) {
                    ChatMessage message = new ChatMessage(username, messageText);
                    out.writeObject(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return t;
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception{
        if (args.length != 1) {
            System.err.println("Usage: java zebrachat.client.ZebraClient <host name>");
            System.exit(1);
        }
        String hostName = args[0];

        Scanner scanner = new Scanner(System.in);
        System.out.print("username: ");
        String username = scanner.next();
        System.out.print("password: ");
        String password = scanner.next();

        new ZebraClient(hostName, username, password).run();
    }
}
