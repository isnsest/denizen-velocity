package com.isnsest.denizen.velocity.packets.in;

import com.isnsest.denizen.velocity.DenizenConnection;
import com.isnsest.denizen.velocity.PacketIn;
import io.netty.buffer.ByteBuf;

public class ControlProxyCommandPacketIn extends PacketIn {

    @Override
    public String getName() {
        return "ControlProxyCommand";
    }

    @Override
    public void process(DenizenConnection connection, ByteBuf data) {
        if (data.readableBytes() < 1) {
            connection.fail("Invalid ControlProxyCommandPacket (bytes available: " + data.readableBytes() + ")");
            return;
        }
        byte b = data.readByte();
        connection.controlsProxyCommand = (b != 0);
    }
}