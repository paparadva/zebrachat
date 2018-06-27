package zebrachat.protocol;

import java.io.Serializable;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Objects;

public class ChatMessage implements Serializable {
    private LocalDateTime receiptTime;
    private String username;
    private String text;

    public ChatMessage(String username, String text) {
        this.username = username;
        this.text = text;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getReceiptTime() {
        return receiptTime;
    }

    public void setReceiptTime(LocalDateTime receiptTime) {
        this.receiptTime = receiptTime;
    }

    public UserStatus getStatus() {
        return UserStatus.NOTHING;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(receiptTime, that.receiptTime) &&
                Objects.equals(username, that.username) &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(receiptTime, username, text);
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "receiptTime=" + receiptTime +
                ", username='" + username + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
