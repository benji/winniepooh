import com.github.benji.winnie.net.netty.NettyServer;

public class NettyServerTest {
	
	public static void main(String[] args) throws Exception {

		int port = 1234;

		NettyServer server = new NettyServer() {
			@Override
			public String handleRequest(String request) {
				System.out.println("Server echo " + request);
				return request;
			}

		};
		server.start(port);
		System.out.println("SERVER ENDS");

	}

}
