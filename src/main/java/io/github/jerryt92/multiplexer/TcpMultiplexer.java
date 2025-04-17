package io.github.jerryt92.multiplexer;

import io.github.jerryt92.multiplexer.conf.ConfigReader;
import io.github.jerryt92.multiplexer.forward.RequestHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class TcpMultiplexer {
    private static final Logger log = LogManager.getLogger(TcpMultiplexer.class);

    public static void main(String[] args) {
        ConfigReader.ServerConfig serverConfig = ConfigReader.INSTANCE.getAppConfig().getServer();
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ChannelHandler channelHandler = new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new RequestHandler(workerGroup));
                }
            };
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(channelHandler)
                    // 设置并发连接数
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .bind(serverConfig.getHost(), serverConfig.getPort())
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            log.info("TcpMultiplexer started at port {}", serverConfig.getPort());
                        } else {
                            log.error("Failed to start TcpMultiplexer", future.cause());
                        }
                    })
                    .sync()
                    .channel()
                    .closeFuture()
                    .sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}