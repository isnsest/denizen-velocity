package com.isnsest.denizen.velocity;

import com.isnsest.denizen.velocity.packets.in.*;
import com.isnsest.denizen.velocity.packets.out.*;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Plugin(id = "denizen-velocity", name = "denizen-velocity", version = "1.1", authors = {"isnsest"})
public class DenizenVelocity {

    public static DenizenVelocity instance;
    public final ProxyServer server;
    public final Logger logger;
    public static Metrics.Factory metricsFactory;
    public static YamlConfig config;

    private static int DEPENIZEN_PORT = 0;
    // public static boolean proxyCommandNoDup = false;
    // public static long proxyCommandId = 1;

    public HashMap<Integer, PacketIn> packets = new HashMap<>();
    private final List<DenizenConnection> connections = new ArrayList<>();
    public HashMap<Long, CompletableFuture<String>> proxyCommandWaiters = new HashMap<>();

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @Inject
    public DenizenVelocity(ProxyServer server,
                           Logger logger,
                           @DataDirectory Path dataDir,
                           Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.metricsFactory = metricsFactory;
        instance = this;
        config = new YamlConfig(dataDir);
        DEPENIZEN_PORT = config.getPort();
    }

    public void addConnection(DenizenConnection connection) {
        synchronized (connections) { connections.add(connection); }
    }

    public void removeConnection(DenizenConnection connection) {
        synchronized (connections) { connections.remove(connection); }
    }

    public List<DenizenConnection> getConnections() {
        synchronized (connections) { return new ArrayList<>(connections); }
    }

    public DenizenConnection getConnectionByName(String name) {
        name = name.toLowerCase(Locale.ENGLISH);
        for (DenizenConnection connection : getConnections()) {
            if (connection.thisServer != null) {
                String serverName = connection.thisServer.getServerInfo().getName();
                if (serverName.toLowerCase(Locale.ENGLISH).equals(name)) {
                    return connection;
                }
            }
        }
        return null;
    }

    public void registerPackets() {
        packets.put(1, new KeepAlivePacketIn());
        packets.put(MyInfoPacketIn.PACKET_ID, new MyInfoPacketIn());
        packets.put(12, new ControlProxyPingPacketIn());
        packets.put(13, new ProxyPingResultPacketIn());
        packets.put(14, new RedirectPacketIn());
        packets.put(15, new ExecuteCommandPacketIn());
        packets.put(16, new ControlProxyCommandPacketIn());
        packets.put(17, new ProxyCommandResultPacketIn());
        packets.put(18, new ExecutePlayerCommandPacketIn());
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Loading...");
        Metrics metrics = metricsFactory.make(this, 28531);
        registerPackets();
        startServer();

        server.getScheduler().buildTask(this, () -> {
            long curTime = System.currentTimeMillis();
            KeepAlivePacketOut packet = new KeepAlivePacketOut();
            for (DenizenConnection connection : getConnections()) {
                if (connection.thisServer == null) continue;
                if (curTime > connection.lastPacketReceived + 20 * 1000) {
                    connection.fail("Connection time out.");
                } else {
                    connection.sendPacket(packet);
                }
            }
        }).repeat(1, TimeUnit.SECONDS).schedule();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }

    private void startServer() {
        new Thread(() -> {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) {
                                InetSocketAddress remoteAddress = ch.remoteAddress();
                                String remoteIp = remoteAddress.getAddress().getHostAddress();

                                RegisteredServer matchedServer = null;
                                for (RegisteredServer rs : server.getAllServers()) {
                                    String serverIp = rs.getServerInfo().getAddress().getAddress().getHostAddress();
                                    if (serverIp.equals(remoteIp)) {
                                        matchedServer = rs;
                                        break;
                                    }
                                    if ((remoteIp.equals("127.0.0.1") || remoteIp.equals("0:0:0:0:0:0:0:1"))
                                            && (rs.getServerInfo().getAddress().getAddress().isLoopbackAddress())) {
                                        matchedServer = rs;
                                        break;
                                    }
                                }

                                if (matchedServer == null) {
                                    logger.warn("BLOCKED connection from " + remoteIp);
                                    ch.close();
                                    return;
                                }

                                logger.info("Accepted connection from " + remoteIp + " as server: " + matchedServer.getServerInfo().getName());

                                DenizenConnection depenConnection = new DenizenConnection();
                                depenConnection.thisServer = matchedServer;
                                depenConnection.build(ch, remoteAddress.getAddress());
                            }
                        });

                ChannelFuture f = b.bind(DEPENIZEN_PORT).sync();
                logger.info("Server started on port " + DEPENIZEN_PORT);
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                logger.error("Could not bind port " + DEPENIZEN_PORT + ". Is it already in use?", e);
            }
        }).start();
    }

    @Subscribe
    public void onPlayerJoin(PostLoginEvent event) {
        PlayerJoinPacketOut packet = new PlayerJoinPacketOut();
        packet.name = event.getPlayer().getUsername();
        packet.uuid = event.getPlayer().getUniqueId();
        packet.ip = event.getPlayer().getRemoteAddress().toString();
        event.getPlayer().getVirtualHost().ifPresent(host -> packet.host = host.getHostString());
        broadcastPacket(packet);
    }

    @Subscribe
    public void onPlayerQuit(DisconnectEvent event) {
        PlayerQuitPacketOut packet = new PlayerQuitPacketOut();
        packet.name = event.getPlayer().getUsername();
        packet.uuid = event.getPlayer().getUniqueId();
        packet.ip = event.getPlayer().getRemoteAddress().toString();
        broadcastPacket(packet);
    }

    @Subscribe
    public void onServerSwitch(ServerConnectedEvent event) {
        PlayerSwitchServerPacketOut packet = new PlayerSwitchServerPacketOut();
        packet.name = event.getPlayer().getUsername();
        packet.uuid = event.getPlayer().getUniqueId();
        packet.newServer = event.getServer().getServerInfo().getName();
        broadcastPacket(packet);
    }

    @Subscribe
    public void onProxyPing(ProxyPingEvent event) {
        for (DenizenConnection connection : getConnections()) {
            if (connection.controlsProxyPing && connection.isValid) {
                long id = connection.proxyPingId++;

                CompletableFuture<ServerPing> future = new CompletableFuture<>();
                connection.proxyPingWaiters.put(id, future);

                ServerPing currentPing = event.getPing();
                ProxyPingPacketOut packet = new ProxyPingPacketOut();
                packet.id = id;
                packet.address = event.getConnection().getRemoteAddress().toString();

                packet.currentPlayers = currentPing.getPlayers().isPresent() ? currentPing.getPlayers().get().getOnline() : 0;
                packet.maxPlayers = currentPing.getPlayers().isPresent() ? currentPing.getPlayers().get().getMax() : 0;
                packet.motd = LegacyComponentSerializer.legacySection().serialize(currentPing.getDescriptionComponent());
                packet.protocol = currentPing.getVersion().getProtocol();
                packet.version = currentPing.getVersion().getName();

                connection.sendPacket(packet);

                try {
                    ServerPing denizenPing = future.get(1500, TimeUnit.MILLISECONDS);

                    ServerPing.Version finalVersion = new ServerPing.Version(
                            currentPing.getVersion().getProtocol(),
                            denizenPing.getVersion().getName()
                    );

                    ServerPing.Builder finalPing = denizenPing.asBuilder();
                    finalPing.version(finalVersion);

                    event.setPing(finalPing.build());

                } catch (Exception e) {
                    connection.proxyPingWaiters.remove(id);
                }

                break;
            }
        }
    }

    public void broadcastPacket(PacketOut packet) {
        for (DenizenConnection connection : getConnections()) {
            connection.sendPacket(packet);
        }
    }
}