package com.github.benji.winnie.net.netty;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SynchronousNettyClient extends NettyClient {

	public SynchronousNettyClient(String host, int port) {
		super(host, port);
	}

	/*
	 * One at a time only.
	 */
	public synchronized String sendSynchronously(String request) throws Exception {
		requestLatch = new CountDownLatch(1);

		super.send(request);

		if (!requestLatch.await(MAX_REQUEST_TIME_SECONDS, TimeUnit.SECONDS)) {
			throw new IllegalStateException("No response within " + MAX_REQUEST_TIME_SECONDS + " seconds.");
		}

		return serverResponse;
	}

	@Override
	protected void onReceivingServerResponse(String str) {
		super.onReceivingServerResponse(str);
		serverResponse = str;
		requestLatch.countDown();
	}
}
