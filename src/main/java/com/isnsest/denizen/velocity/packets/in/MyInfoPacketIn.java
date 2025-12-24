package com.isnsest.denizen.velocity.packets.in;

import com.isnsest.denizen.velocity.DenizenConnection;
import com.isnsest.denizen.velocity.DenizenVelocity;
import com.isnsest.denizen.velocity.PacketIn;
import com.isnsest.denizen.velocity.packets.out.YourInfoPacketOut;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.netty.buffer.ByteBuf;

public class MyInfoPacketIn extends PacketIn {

    public static final int PACKET_ID = 11;

    @Override
    public String getName() {
        return "MyInfo";
    }

    @Override
    public void process(DenizenConnection connection, ByteBuf data) {
        if (data.readableBytes() < 4) {
            connection.fail("Invalid MyInfoPacket (bytes available: " + data.readableBytes() + ")");
            return;
        }
        int port = data.readInt();
        connection.serverPort = port;

        for (RegisteredServer server : DenizenVelocity.instance.server.getAllServers()) {
            if (server.getServerInfo().getAddress().getAddress().equals(connection.serverAddress)
                    && server.getServerInfo().getAddress().getPort() == port) {
                connection.thisServer = server;
                break;
            }
        }

        if (connection.thisServer == null) {
            connection.fail("Invalid MyInfoPacket (unknown server, gave port '" + port + "'). Make sure this server is in velocity.toml");
            return;
        }

        connection.sendPacket(new YourInfoPacketOut(connection.thisServer.getServerInfo().getName()));
        connection.broadcastIdentity();
    }
}