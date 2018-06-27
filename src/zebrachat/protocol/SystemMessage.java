package zebrachat.protocol;

import java.io.Serializable;

public class SystemMessage implements Serializable{
    private String message;
    private Status status;

    public SystemMessage(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        OK, ERROR
    }
}
