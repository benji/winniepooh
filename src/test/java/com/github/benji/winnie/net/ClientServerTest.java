package com.github.benji.winnie.net;

import javax.net.ssl.SSLContext;

import com.github.benji.ssl.tests.utils.SSLTestsUtils;
import com.github.benji.ssl.tests.utils.TestCertificate;
import com.github.benji.winnie.net.netty.NettyClient;
import com.github.benji.winnie.net.netty.NettyServer;

import junit.framework.TestCase;

public class ClientServerTest extends TestCase {

	public void testClientServerSimple() throws Exception {
		int port = 1337;
		boolean useSSL = false;

		NettyServer server = new NettyServer() {
			@Override
			public String handleRequest(String request) {
				System.out.println("Server echo " + request);
				return request;
			}

		};

		NettyClient client = new NettyClient("localhost", port);

		if (useSSL) {
			TestCertificate cert = SSLTestsUtils.createSelfSignedCertificate("winnie");
			SSLContext serverSSLContext = SSLContext.getInstance("TLS");
			SSLTestsUtils.initSSLContext(serverSSLContext, cert);
			server.setSSLContext(serverSSLContext);

			SSLContext clientSSLContext = SSLContext.getInstance("TLS");
			SSLTestsUtils.initSSLContext(clientSSLContext, null, cert);
			client.setSslContext(clientSSLContext);
		}

		Thread t = new Thread() {
			@Override
			public void run() {
				try {
					server.start(port);
					System.out.println("SERVER ENDS");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					server.closeQuietly();
				}
			}
		};
		t.start();

		client.connect();
		
		new Thread() {
			@Override
			public void run() {
				try {
					while (true) {
						client.send("hello");
						Thread.sleep(1000);
					}
				} catch (Exception e) {
				}
			}
		}.start();

		Thread.sleep(1000000);
		System.out.println("End main.");

	}

}
