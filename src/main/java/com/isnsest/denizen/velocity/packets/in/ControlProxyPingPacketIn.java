package com.isnsest.denizen.velocity.packets.in;

import com.isnsest.denizen.velocity.DenizenConnection;
import com.isnsest.denizen.velocity.PacketIn;
import io.netty.buffer.ByteBuf;

public class ControlProxyPingPacketIn extends PacketIn {

    @Override
    public String getName() {
        return "ControlProxyPing";
    }

    @Override
    public void process(DenizenConnection connection, ByteBuf data) {
        if (data.readableBytes() < 1) {
            connection.fail("Invalid ControlProxyPingPacket (bytes available: " + data.readableBytes() + ")");
            return;
        }
        byte b = data.readByte();
        connection.controlsProxyPing = (b != 0);
    }
}