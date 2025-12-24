package com.isnsest.denizen.velocity.packets.out;

import com.isnsest.denizen.velocity.PacketOut;
import io.netty.buffer.ByteBuf;

public class RedirectedPacketOut extends PacketOut {

    public RedirectedPacketOut(int id, byte[] data) {
        this.id = id;
        this.data = data;
    }

    public int id;

    public byte[] data;

    @Override
    public int getPacketId() {
        return id;
    }

    @Override
    public void writeTo(ByteBuf buf) {
        buf.writeBytes(data);
    }
}
