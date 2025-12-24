package com.isnsest.denizen.velocity.packets.out;

import com.isnsest.denizen.velocity.PacketOut;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class PlayerJoinPacketOut extends PacketOut {

    public String name;
    public UUID uuid;
    public String ip;
    public String host;

    @Override
    public int getPacketId() {
        return 53;
    }

    @Override
    public void writeTo(ByteBuf buf) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        writeString(buf, name);
        writeString(buf, ip);
        writeString(buf, host == null ? "" : host);
    }
}