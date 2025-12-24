package com.isnsest.denizen.velocity;

import com.isnsest.denizen.velocity.packets.out.AddServerPacketOut;
import com.isnsest.denizen.velocity.packets.out.RemoveServerPacketOut;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class DenizenConnection extends ChannelInboundHandlerAdapter {

    public RegisteredServer thisServer;
    public HashMap<Long, CompletableFuture<ServerPing>> proxyPingWaiters = new HashMap<>();

    public Channel channel;
    public InetAddress serverAddress;
    public String connectionName;
    public boolean isValid = false;

    public ByteBuf packetBuffer;
    public int waitingLength;
    public int packetId;
    public int serverPort;

    public Stage currentStage = Stage.AWAIT_HEADER;
    public long lastPacketReceived = 0;
    private boolean handshakeFound = false;

    public boolean controlsProxyPing = false;
    public boolean controlsProxyCommand = false;
    public long proxyPingId = 1;

    public HashMap<Long, ProxyPingEvent> proxyEventMap = new HashMap<>();

    public enum Stage { AWAIT_HEADER, AWAIT_DATA }

    public void build(Channel channel, InetAddress address) {
        this.serverAddress = address;
        this.channel = channel;
        this.connectionName = channel.remoteAddress().toString();

        try {
            while (channel.pipeline().first() != null) channel.pipeline().removeFirst();
        } catch (Exception ignored) {}

        NettyExceptionHandler nettyEx = new NettyExceptionHandler();
        nettyEx.connection = this;

        channel.pipeline().addLast("depenizen_handler", this);
        channel.pipeline().addLast("exception_handler", nettyEx);

        DenizenVelocity.instance.addConnection(this);
        isValid = true;
    }

    public void fail(String reason) {
        if (isValid) {
            isValid = false;
            DenizenVelocity.instance.logger.info("Connection " + connectionName + " failed: " + reason);
            if (channel != null && channel.isOpen()) channel.close();
        }
    }

    public synchronized void sendPacket(PacketOut packet) {
        try {
            if (channel == null || !channel.isActive()) return;
            ByteBuf buf = channel.alloc().buffer();
            packet.writeTo(buf);
            ByteBuf header = channel.alloc().buffer();
            header.writeInt(buf.writerIndex());
            header.writeInt(packet.getPacketId());
            channel.writeAndFlush(header);
            channel.writeAndFlush(buf);
        } catch (Exception ignored) {}
    }

    public void broadcastIdentity() {
        DenizenVelocity.instance.server.getScheduler().buildTask(DenizenVelocity.instance, () -> {
            if (thisServer != null) DenizenVelocity.instance.broadcastPacket(new AddServerPacketOut(thisServer.getServerInfo().getName()));
        }).schedule();
    }

    public void broadcastRemoval() {
        DenizenVelocity.instance.server.getScheduler().buildTask(DenizenVelocity.instance, () -> {
            if (thisServer != null) DenizenVelocity.instance.broadcastPacket(new RemoveServerPacketOut(thisServer.getServerInfo().getName()));
        }).schedule();
    }

    public void reallocateBuf(ChannelHandlerContext ctx) {
        if (packetBuffer == null) return;
        ByteBuf newBuf = ctx.alloc().buffer(packetBuffer.readableBytes() + 32);
        newBuf.writeBytes(packetBuffer);
        packetBuffer.release();
        packetBuffer = newBuf;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        packetBuffer = ctx.alloc().buffer(32);
        lastPacketReceived = System.currentTimeMillis();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        if (packetBuffer != null) {
            packetBuffer.release();
            packetBuffer = null;
        }
        DenizenVelocity.instance.removeConnection(this);
        broadcastRemoval();
        proxyEventMap.clear();
        proxyPingWaiters.clear();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        lastPacketReceived = System.currentTimeMillis();
        try {
            ByteBuf m = (ByteBuf) msg;
            if (packetBuffer == null) packetBuffer = ctx.alloc().buffer(32);
            packetBuffer.writeBytes(m);
            m.release();

            if (!handshakeFound) {
                while (packetBuffer.readableBytes() >= 8) {
                    packetBuffer.markReaderIndex();
                    int len = packetBuffer.readInt();
                    int id = packetBuffer.readInt();
                    if (DenizenVelocity.instance.packets.containsKey(id)) {
                        packetBuffer.resetReaderIndex();
                        handshakeFound = true;
                        DenizenVelocity.instance.logger.info("Connected server: " + connectionName);
                        break;
                    } else {
                        packetBuffer.resetReaderIndex();
                        packetBuffer.readByte();
                    }
                }
                if (!handshakeFound) {
                    reallocateBuf(ctx);
                    return;
                }
            }

            while (true) {
                if (currentStage == Stage.AWAIT_HEADER) {
                    if (packetBuffer.readableBytes() < 8) break;
                    waitingLength = packetBuffer.readInt();
                    packetId = packetBuffer.readInt();
                    currentStage = Stage.AWAIT_DATA;
                } else {
                    if (packetBuffer.readableBytes() < waitingLength) break;

                    PacketIn packet = DenizenVelocity.instance.packets.get(packetId);
                    if (packet != null) {
                        packet.process(this, packetBuffer);
                    } else {
                        packetBuffer.skipBytes(waitingLength);
                    }

                    currentStage = Stage.AWAIT_HEADER;
                }
            }

            reallocateBuf(ctx);

        } catch (Exception e) {
            DenizenVelocity.instance.logger.error("Read error for " + connectionName, e);
            fail("Internal error");
        }
    }
}