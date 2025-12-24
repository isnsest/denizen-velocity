package com.isnsest.denizen.velocity.packets.in;

import com.isnsest.denizen.velocity.DenizenConnection;
import com.isnsest.denizen.velocity.PacketIn;
import io.netty.buffer.ByteBuf;

public class KeepAlivePacketIn extends PacketIn {

    @Override
    public String getName() {
        return "KeepAlive";
    }

    @Override
    public void process(DenizenConnection connection, ByteBuf data) {
    }
}