package com.isnsest.denizen.velocity.packets.out;

import com.isnsest.denizen.velocity.PacketOut;
import io.netty.buffer.ByteBuf;

public class YourInfoPacketOut extends PacketOut {

    public YourInfoPacketOut(String name) {
        this.name = name;
    }

    public String name;

    @Override
    public int getPacketId() {
        return 50;
    }

    @Override
    public void writeTo(ByteBuf buf) {
        writeString(buf, name);
    }
}
