package com.isnsest.denizen.velocity.packets.out;

import com.isnsest.denizen.velocity.PacketOut;
import io.netty.buffer.ByteBuf;

public class KeepAlivePacketOut extends PacketOut {

    @Override
    public int getPacketId() {
        return 1;
    }

    @Override
    public void writeTo(ByteBuf buf) {
    }
}
