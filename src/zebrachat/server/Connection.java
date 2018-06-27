package zebrachat.server;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.plugin.dom.exception.InvalidStateException;
import zebrachat.protocol.*;

public class Connection extends Thread implements Closeable{
    private Socket socket;
    private ZebraServer server;
    private InetAddress userAddress;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private static Logger log = ZebraServer.log;
    private String username;

    Connection(Socket socket, ZebraServer server) throws IOException {
        this.socket = socket;
        this.server = server;

        try {
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            log.throwing(this.getClass().getName(), "zebrachat.server.Connection()", e);
            throw e;
        }

        userAddress = socket.getInetAddress();
        assert userAddress != null;
    }

    public LoginRequest readLoginRequest() throws IOException, ClassNotFoundException {
        if(in == null) throw new InvalidStateException("Input stream is null");
        LoginRequest request;
        try {
            request = (LoginRequest) in.readObject();
            username = request.getUsername();
        } catch (IOException | ClassNotFoundException e) {
            log.throwing(this.getClass().getName(), "readLoginRequest", e);
            throw e;
        }
        return request;
    }

    @Override
    public void run() {
        try {
            ChatMessage message;
            try {
                while((message = (ChatMessage)in.readObject()) != null) {
                    message.setReceiptTime(LocalDateTime.now());
                    server.broadcast(message);
                }
            } catch (ClassNotFoundException e) {
                log.log(Level.SEVERE, "Error while decoding message\n ", e);
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, "IOException: ", e);

        } finally {
            server.processUserLeft(username);
            close();
        }
    }

    @Override
    public void close() {
        try {
            log.info("Closing connection socket");
            //server.processUserLeft(username);
            socket.close();
        } catch (IOException e) {
            log.log(Level.WARNING,"IOException while closing socket: ", e);
        }
    }

    public void sendMessage(ChatMessage message) throws IOException {
        assert out != null & message != null;
        log.info("Writing message");
        out.writeObject(message);
    }

    public void sendMessage(SystemMessage message) throws IOException {
        assert out != null & message != null;
        log.info("Writing system message");
        out.writeObject(message);
    }

    public String getUsername() {
        return username;
    }
}
