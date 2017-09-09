package com.github.benji.winnie.net.netty;

import javax.net.ssl.SSLContext;

import com.github.benji.winnie.net.TCPServer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public abstract class NettyServer implements TCPServer {

	SSLContext sslContext = null;
	EventLoopGroup bossGroup = new NioEventLoopGroup(3);
	EventLoopGroup workerGroup = new NioEventLoopGroup(4);

	public NettyServer() {
	}

	public void setSSLContext(SSLContext serverSSLContext) {
		this.sslContext = serverSSLContext;
	}

	public void start(int port) throws Exception {
		System.out.println("Starting server on port " + port);
		
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup);
		bootstrap.channel(NioServerSocketChannel.class);
		bootstrap.childHandler(new Init());
		bootstrap.bind(port).sync().channel().closeFuture().sync();

		System.out.println("Start finished.");
	}

	public void closeQuietly() {
		System.out.println("Closing server");
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}

	private class Init extends ChannelInitializer {
		@Override
		protected void initChannel(Channel ch) throws Exception {
			System.out.println("a");
			// engine.setUseClientMode(true);
			ch.pipeline().addLast(new ShoutyHandler());
			if (sslContext != null) {
				ch.pipeline().addLast(new SslHandler(sslContext.createSSLEngine(), true));
			}
			System.out.println("b");
		}
	}

	private class ShoutyHandler extends ChannelInboundHandlerAdapter {
		// @Override
		// public void channelActive(ChannelHandlerContext ctx) throws Exception
		// {
		// System.out.println("server chan active.");
		// }

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			System.out.println(1);
			ByteBuf buf = (ByteBuf) msg;
			try {
				String in = buf.toString(CharsetUtil.UTF_8);
				System.out.println("server received " + in);
				ctx.writeAndFlush(Unpooled.copiedBuffer(in, CharsetUtil.UTF_8));
			} finally {
				ReferenceCountUtil.release(msg);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			cause.printStackTrace();
			ctx.close();
		}
	}

	public abstract String handleRequest(String request);

}