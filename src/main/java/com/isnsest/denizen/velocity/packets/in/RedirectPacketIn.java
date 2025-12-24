package com.isnsest.denizen.velocity.packets.in;

import com.isnsest.denizen.velocity.DenizenConnection;
import com.isnsest.denizen.velocity.DenizenVelocity;
import com.isnsest.denizen.velocity.PacketIn;
import com.isnsest.denizen.velocity.packets.out.RedirectedPacketOut;
import io.netty.buffer.ByteBuf;

public class RedirectPacketIn extends PacketIn {

    @Override
    public String getName() {
        return "Redirect";
    }

    @Override
    public void process(DenizenConnection connection, ByteBuf data) {
        if (data.readableBytes() < 12) {
            connection.fail("Invalid RedirectPacket (bytes available: " + data.readableBytes() + ")");
            return;
        }
        String serverName = readString(connection, data, "serverName");
        if (serverName == null) {
            return;
        }
        int newPacketLen = data.readInt();
        if (data.readableBytes() < newPacketLen || newPacketLen < 0) {
            connection.fail("Invalid RedirectPacket (packet bytes requested: " + newPacketLen + ")");
            return;
        }
        int newId = data.readInt();
        byte[] newPacket = new byte[newPacketLen];
        data.readBytes(newPacket, 0, newPacketLen);

        DenizenConnection targetConnection = DenizenVelocity.instance.getConnectionByName(serverName);

        if (targetConnection == null) {
            DenizenVelocity.instance.logger.warn("(RedirectPacket from '" + connection.connectionName + "'): Invalid server name '" + serverName + "'");
            return;
        }
        targetConnection.sendPacket(new RedirectedPacketOut(newId, newPacket));
    }
}