package io.github.jerryt92.multiplexer.forward;

import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class RequestHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(RequestHandler.class);
    private final TcpForwardRule tcpForwardRule;
    private final EventLoopGroup workerGroup;

    public RequestHandler(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
        this.tcpForwardRule = new TcpForwardRule();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        try {
            ForwardTarget forwardTarget = tcpForwardRule.getRoute(ctx, msg);
            if (forwardTarget == null || forwardTarget.isReject()) {
                ctx.channel().close();
                return;
            }
            if (ProxyChannelCache.getChannelClientCache().containsKey(ctx.channel())) {
                Channel channel = ProxyChannelCache.getChannelClientCache().get(ctx.channel());
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(msg);
                    return;
                }
                ProxyChannelCache.getChannelClientCache().remove(ctx.channel());
            }
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.handler(new ResponseHandler(ctx.channel()));
            ChannelFuture f = b.connect(forwardTarget.getHost(), forwardTarget.getPort()).sync();
            Channel channel = f.channel();
            ProxyChannelCache.getChannelClientCache().put(ctx.channel(), channel);
            channel.writeAndFlush(msg);
        } catch (Exception e) {
            exceptionCaught(ctx, e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("", cause);
        closeOnFlush(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ProxyChannelCache.getChannelClientCache().remove(ctx.channel());
        ProxyChannelCache.getChannelRouteCache().remove(ctx.channel());
        super.channelInactive(ctx);
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
