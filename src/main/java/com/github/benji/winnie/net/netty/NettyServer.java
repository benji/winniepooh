package com.github.benji.winnie.net.netty;

import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.github.benji.winnie.net.TCPServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public abstract class NettyServer implements TCPServer {

	SSLContext sslContext = null;
	EventLoopGroup bossGroup = new NioEventLoopGroup();
	EventLoopGroup workerGroup = new NioEventLoopGroup();
	Channel channel;

	public void setSSLContext(SSLContext serverSSLContext) {
		this.sslContext = serverSSLContext;
	}

	public void start(int port) throws Exception {
		System.out.println("Server starting on port " + port);

		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup);
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.childHandler(new ServerChannelInitializer());

		channel = bootstrap.bind(port).sync().channel();

		System.out.println("Server ready on port " + getPort());
	}

	public void closeQuietly() {
		System.out.println("Closing server");
		try {
			channel.close().sync();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		System.out.println("Server has closed.");
	}

	public int getPort() {
		InetSocketAddress addr = (InetSocketAddress) channel.localAddress();
		return addr.getPort();
	}

	private class ShoutyHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			ByteBuf buf = (ByteBuf) msg;
			try {
				String in = buf.toString(CharsetUtil.UTF_8);
				System.out.println("Server processing request data: " + in);
				ctx.writeAndFlush(Unpooled.copiedBuffer(in, CharsetUtil.UTF_8));
			} finally {
				ReferenceCountUtil.release(msg);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			System.out.println("Server channel exception received: " + cause.getMessage());
			ctx.close();
		}
	}

	public abstract String handleRequest(String request);

	private class ServerChannelInitializer extends ChannelInitializer {
		@Override
		protected void initChannel(Channel ch) throws Exception {
			if (sslContext != null) {
				System.out.println("Server is using SSL");
				SSLEngine sslEngine = sslContext.createSSLEngine();
				sslEngine.setUseClientMode(false);
				ch.pipeline().addLast(new SslHandler(sslEngine));
			}
			ch.pipeline().addLast(new ShoutyHandler());
		}
	}

}