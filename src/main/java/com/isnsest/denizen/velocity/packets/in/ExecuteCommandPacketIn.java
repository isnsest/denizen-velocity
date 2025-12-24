package com.isnsest.denizen.velocity.packets.in;

import com.isnsest.denizen.velocity.DenizenConnection;
import com.isnsest.denizen.velocity.DenizenVelocity;
import com.isnsest.denizen.velocity.PacketIn;
import io.netty.buffer.ByteBuf;

public class ExecuteCommandPacketIn extends PacketIn {

    @Override
    public String getName() {
        return "ExecuteCommand";
    }

    @Override
    public void process(DenizenConnection connection, ByteBuf data) {
        String command = readString(connection, data, "command");
        if (command == null) return;

        DenizenVelocity.instance.server.getCommandManager().executeAsync(
                DenizenVelocity.instance.server.getConsoleCommandSource(),
                command
        );
    }
}