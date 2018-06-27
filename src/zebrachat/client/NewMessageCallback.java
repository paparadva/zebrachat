package zebrachat.client;

import zebrachat.protocol.ChatMessage;

public interface NewMessageCallback {
    void processChatMessage(ChatMessage message);
}
