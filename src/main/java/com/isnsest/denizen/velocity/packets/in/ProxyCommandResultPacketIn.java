package com.isnsest.denizen.velocity.packets.in;

import com.isnsest.denizen.velocity.DenizenConnection;
import com.isnsest.denizen.velocity.DenizenVelocity;
import com.isnsest.denizen.velocity.PacketIn;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;

public class ProxyCommandResultPacketIn extends PacketIn {

    @Override
    public String getName() {
        return "ProxyCommandResult";
    }

    @Override
    public void process(DenizenConnection connection, ByteBuf data) {
        if (data.readableBytes() < 8 + 4) {
            connection.fail("Invalid ProxyCommandResultPacket (bytes available: " + data.readableBytes() + ")");
            return;
        }
        long id = data.readLong();
        String result = readString(connection, data, "result");
        if (result == null) {
            return;
        }
        CompletableFuture<String> future = DenizenVelocity.instance.proxyCommandWaiters.get(id);
        if (future == null) {
            return;
        }
        future.complete(result.length() > 0 ? result : null);
        DenizenVelocity.instance.proxyCommandWaiters.remove(id);
    }
}