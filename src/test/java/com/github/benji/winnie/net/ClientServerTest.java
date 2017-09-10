package com.github.benji.winnie.net;

import javax.net.ssl.SSLContext;

import com.github.benji.ssl.tests.utils.SSLTestsUtils;
import com.github.benji.ssl.tests.utils.TestCertificate;
import com.github.benji.winnie.net.netty.NettyServer;
import com.github.benji.winnie.net.netty.SynchronousNettyClient;

import junit.framework.TestCase;

public class ClientServerTest extends TestCase {

	public void testSynchronousClientServerCommunication() throws Exception {
		boolean useSSL = true;
		TestCertificate cert = null;

		NettyServer echoServer = new NettyServer() {
			@Override
			public String handleRequest(String request) {
				System.out.println("Server echo " + request);
				return request;
			}
		};

		if (useSSL) {
			cert = SSLTestsUtils.createSelfSignedCertificate("winnie");
			SSLContext serverSSLContext = SSLContext.getInstance("TLS");
			SSLTestsUtils.initSSLContext(serverSSLContext, cert);
			echoServer.setSSLContext(serverSSLContext);
		}

		echoServer.start(0);

		SynchronousNettyClient client = new SynchronousNettyClient("localhost", echoServer.getPort());

		if (useSSL) {
			SSLContext clientSSLContext = SSLContext.getInstance("TLS");
			SSLTestsUtils.initSSLContext(clientSSLContext, null, cert);
			client.setSslContext(clientSSLContext);
		}

		client.connect();

		try {
			for (int i = 0; i < 10; i++) {
				String msg = "ping #" + i;
				assertEquals(msg, client.sendSynchronously(msg));
			}
		} finally {
			client.closeQuietly();
			echoServer.closeQuietly();
		}
	}

}
