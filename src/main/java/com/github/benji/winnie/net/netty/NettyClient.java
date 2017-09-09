package com.github.benji.winnie.net.netty;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.CharsetUtil;

public class NettyClient {

	String host;
	int port;

	EventLoopGroup workerGroup = new NioEventLoopGroup(5);
	SSLContext sslContext = null;
	ChannelHandlerContext channelHandlerContext;

	public NettyClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public void closeQuietly() {
		System.out.println("Closing client");
		workerGroup.shutdownGracefully();
	}

	public void connect() throws InterruptedException {
		System.out.println("Client connection...");
		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new ClientHandler());
				if (sslContext != null) {
					SSLEngine engine = sslContext.createSSLEngine();
					ch.pipeline().addLast(new SslHandler(engine, true));
				}
			}
		});

		// Start the client.
		ChannelFuture f = b.connect(host, port).sync();// .channel().closeFuture().sync();

		// Wait until the connection is closed.
		// f.channel().closeFuture().sync();
	}

	public void send(String request) throws Exception {
		System.out.println("client sending " + request);
		channelHandlerContext.writeAndFlush(Unpooled.copiedBuffer(request, CharsetUtil.UTF_8)).sync();
		System.out.println("client sent data");
	}

	public class ClientHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			channelHandlerContext = ctx;
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			System.out.println("client read");
			ByteBuf m = (ByteBuf) msg;
			try {
				String str = m.toString(CharsetUtil.UTF_8);
				System.out.println("Client receiving " + str);
				// ctx.close();
			} finally {
				m.release();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			cause.printStackTrace();
			ctx.close();
		}
	}
}
