package com.isnsest.denizen.velocity;

import io.netty.channel.*;

import java.net.SocketAddress;

public class NettyExceptionHandler extends ChannelDuplexHandler {

    public DenizenConnection connection;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        DenizenVelocity.instance.logger.info("Connection " + connection.connectionName + " caught an exception");
        cause.printStackTrace();
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        ctx.connect(remoteAddress, localAddress, promise.addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                DenizenVelocity.instance.logger.info("Connection " + connection.connectionName + " failed to operationComplete");
                future.cause().printStackTrace();
            }
        }));
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) {
        ctx.close(promise.addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                DenizenVelocity.instance.logger.info("Connection " + connection.connectionName + " failed to close");
                future.cause().printStackTrace();
            }
        }));
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ctx.write(msg, promise.addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                DenizenVelocity.instance.logger.info("Connection " + connection.connectionName + " failed to write");
                future.cause().printStackTrace();
            }
        }));
    }
}
