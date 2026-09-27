package net.minecraftforge.client.event;

import net.minecraft.util.IChatComponent;

public class ClientChatReceivedEvent {
    public IChatComponent message;
    public final byte type;

    public ClientChatReceivedEvent(byte type, IChatComponent message) {
        this.type = type;
        this.message = message;
    }
}
