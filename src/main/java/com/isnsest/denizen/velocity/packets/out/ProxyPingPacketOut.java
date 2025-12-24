package com.isnsest.denizen.velocity.packets.out;

import com.isnsest.denizen.velocity.PacketOut;
import io.netty.buffer.ByteBuf;

public class ProxyPingPacketOut extends PacketOut {

    public long id;
    public String address;
    public int currentPlayers;
    public int maxPlayers;
    public String motd;
    public int protocol;
    public String version;

    @Override
    public int getPacketId() {
        return 56;
    }

    @Override
    public void writeTo(ByteBuf buf) {
        buf.writeLong(id);
        writeString(buf, address == null ? "" : address);
        buf.writeInt(currentPlayers);
        buf.writeInt(maxPlayers);
        writeString(buf, motd == null ? "" : motd);
        buf.writeInt(protocol);
        writeString(buf, version == null ? "" : version);
    }
}