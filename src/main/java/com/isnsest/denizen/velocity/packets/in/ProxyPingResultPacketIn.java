package com.isnsest.denizen.velocity.packets.in;

import com.isnsest.denizen.velocity.DenizenConnection;
import com.isnsest.denizen.velocity.PacketIn;
import com.velocitypowered.api.proxy.server.ServerPing;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProxyPingResultPacketIn extends PacketIn {

    @Override
    public String getName() {
        return "ProxyPingResult";
    }

    @Override
    public void process(DenizenConnection connection, ByteBuf data) {
        if (data.readableBytes() < 8 + 4 + 4 + 4) {
            return;
        }

        long id = data.readLong();
        int maxPlayers = data.readInt();
        String version = readString(connection, data, "version");
        String motd = readString(connection, data, "motd");

        if (version == null || motd == null) {
            return;
        }

        int playerListCount = data.readInt();
        if (playerListCount < -1 || playerListCount > 5000) {
            connection.fail("Invalid ProxyPingResultPacket (playerListCount requested: " + playerListCount + ")");
            return;
        }

        List<ServerPing.SamplePlayer> playerInfo = new ArrayList<>();
        if (playerListCount != -1) {
            for (int i = 0; i < playerListCount; i++) {
                String name = readString(connection, data, "name");
                if (name == null) break;

                long idMost = data.readLong();
                long idLeast = data.readLong();
                UUID uuid = new UUID(idMost, idLeast);

                playerInfo.add(new ServerPing.SamplePlayer(name, uuid));
            }
        }

        CompletableFuture<ServerPing> future = connection.proxyPingWaiters.remove(id);

        if (future != null) {
            ServerPing.Builder builder = ServerPing.builder();

            builder.version(new ServerPing.Version(763, version));
            builder.description(LegacyComponentSerializer.legacySection().deserialize(motd));
            builder.maximumPlayers(maxPlayers);
            builder.onlinePlayers(playerInfo.size());
            builder.samplePlayers(playerInfo.toArray(new ServerPing.SamplePlayer[0]));

            future.complete(builder.build());
        }
    }
}