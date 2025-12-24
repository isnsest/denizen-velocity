package com.isnsest.denizen.velocity.packets.in;

import com.isnsest.denizen.velocity.DenizenConnection;
import com.isnsest.denizen.velocity.DenizenVelocity;
import com.isnsest.denizen.velocity.PacketIn;
import com.velocitypowered.api.proxy.Player;
import io.netty.buffer.ByteBuf;

import java.util.Optional;
import java.util.UUID;

public class ExecutePlayerCommandPacketIn extends PacketIn {

    @Override
    public String getName() {
        return "ExecutePlayerCommand";
    }

    @Override
    public void process(DenizenConnection connection, ByteBuf data) {
        String playerStr = readString(connection, data, "player");
        String command = readString(connection, data, "command");
        if (command == null || playerStr == null) {
            return;
        }

        try {
            UUID uuid = UUID.fromString(playerStr);
            Optional<Player> playerObj = DenizenVelocity.instance.server.getPlayer(uuid);

            if (playerObj.isPresent()) {
                DenizenVelocity.instance.server.getCommandManager().executeAsync(playerObj.get(), command);
            }
        } catch (IllegalArgumentException ex) {
        }
    }
}