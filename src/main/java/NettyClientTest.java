
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

public class NettyClientTest {

	public static void main(String[] args) throws Exception {
		NettyClientTest client = new NettyClientTest("localhost", 1234);
		client.connect();
		client.send("test1");
		Thread.sleep(1000);
		client.send("test2");
	}

	String host;
	int port;

	public NettyClientTest(String host, int port) {
		this.host = host;
		this.port = port;
	}

	EventLoopGroup workerGroup = new NioEventLoopGroup(5);

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
			}
		});

		// Start the client.
		ChannelFuture f = b.connect(host, port).sync();
		System.out.println("Client started.");
	}

	public void send(String request) throws Exception {
		System.out.println("client sending " + request);
		clientChannel.writeAndFlush(Unpooled.copiedBuffer(request, CharsetUtil.UTF_8)).sync();
		System.out.println("client sent data");
	}

	ChannelHandlerContext channelHandlerContext;
	Channel clientChannel;

	public class ClientHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			System.out.println("Channel is active.");
			channelHandlerContext = ctx;
			clientChannel = ctx.channel();
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			System.out.println("client read");
			ByteBuf m = (ByteBuf) msg;
			try {
				String str = m.toString(CharsetUtil.UTF_8);
				System.out.println("Client receiving " + str);
//				ctx.close();
			} finally {
				m.release();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			System.out.println("ERROR!");
			cause.printStackTrace();
			ctx.close();
		}
	}
}
