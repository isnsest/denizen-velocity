package com.isnsest.denizen.velocity;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;

public abstract class PacketIn {
    public abstract String getName();
    public abstract void process(DenizenConnection connection, ByteBuf data);

    public String readString(DenizenConnection connection, ByteBuf buf, String label) {
        if (buf.readableBytes() < 4) {
            connection.fail("Invalid " + getName() + " Packet " + label + " length bytes (needed 4)");
            return null;
        }
        int len = buf.readInt();
        if (buf.readableBytes() < len || len < 0) {
            connection.fail("Invalid " + getName() + " Packet " + label + " (bytes requested: " + len + ", bytes available: " + buf.readableBytes() + ")");
            return null;
        }
        byte[] strBytes = new byte[len];
        buf.readBytes(strBytes, 0, len);
        return new String(strBytes, Charsets.UTF_8);
    }
}