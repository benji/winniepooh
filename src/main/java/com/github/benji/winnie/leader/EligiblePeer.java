package com.github.benji.winnie.leader;

import com.github.benji.winnie.peer.Peer;

public class EligiblePeer extends Peer {

	public boolean isLeader() {
		return false;
	}

	public String getLeader() {
		return null;
	}

	public void start() {
//		super.start();
		competeForLeadership();
	}

	private void competeForLeadership() {
		// TODO Auto-generated method stub

	}

}
