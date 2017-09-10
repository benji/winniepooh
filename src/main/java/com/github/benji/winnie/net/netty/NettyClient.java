package com.github.benji.winnie.net.netty;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
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

	EventLoopGroup workerGroup = new NioEventLoopGroup();
	SSLContext sslContext = null;
	Channel channel = null;

	public NettyClient(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void setSslContext(SSLContext sslContext) {
		this.sslContext = sslContext;
	}

	public void closeQuietly() {
		System.out.println("Closing client");
		channel.close().syncUninterruptibly();
		workerGroup.shutdownGracefully();
		System.out.println("Client has closed");
	}

	public void connect() throws InterruptedException {
		System.out.println("Client connection to " + host + ":" + port);
		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.handler(new ClientChannelInitializer());

		this.channel = b.connect(host, port).sync().channel();
	}

	public void send(String request) throws Exception {
		System.out.println("Client sending request " + request);
		channel.writeAndFlush(Unpooled.copiedBuffer(request, CharsetUtil.UTF_8)).sync();
	}

	public class ClientHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			ByteBuf m = (ByteBuf) msg;
			try {
				String str = m.toString(CharsetUtil.UTF_8);
				onReceivingServerResponse(str);
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

	protected void onReceivingServerResponse(String str) {
		System.out.println("Client received data: " + str);
	}

	private class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {
		@Override
		public void initChannel(SocketChannel ch) throws Exception {
			if (sslContext != null) {
				System.out.println("Client is using SSL");
				SSLEngine sslEngine = sslContext.createSSLEngine();
				sslEngine.setUseClientMode(true);
				ch.pipeline().addLast(new SslHandler(sslEngine));
			}
			ch.pipeline().addLast(new ClientHandler());
		}
	}
}
