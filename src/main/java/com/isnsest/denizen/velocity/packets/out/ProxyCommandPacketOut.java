package com.isnsest.denizen.velocity.packets.out;

import com.isnsest.denizen.velocity.PacketOut;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class ProxyCommandPacketOut extends PacketOut {

    public ProxyCommandPacketOut(long id , String sender, UUID senderId, String command) {
        this.id = id;
        this.sender = sender;
        this.command = command;
        this.senderId = senderId;
    }

    public long id;

    public String sender;

    public String command;

    public UUID senderId;

    @Override
    public int getPacketId() {
        return 61;
    }

    @Override
    public void writeTo(ByteBuf buf) {
        buf.writeLong(id);
        writeString(buf, sender);
        writeString(buf, command);
        writeString(buf, senderId == null ? "" : senderId.toString());
    }
}
