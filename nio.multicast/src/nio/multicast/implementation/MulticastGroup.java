package nio.multicast.implementation;

import java.nio.ByteBuffer;
import java.util.HashMap;

import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.multicast.IMulticastDeliverCallback;
import nio.multicast.IMulticastGroup;

public class MulticastGroup implements IMulticastGroup {

	private NioEngine nEngine;
	private IMulticastDeliverCallback callback;
	private HashMap<Integer,NioChannel> members;
	
	
	public MulticastGroup(NioEngine nEngine) {
		this.nEngine = nEngine;
	}
	
	
	public void addMember(Integer n,NioChannel nChannel)
	{
		members.put(n, nChannel);
	}
	
	@Override
	public void setMulticastDeliverCallback(IMulticastDeliverCallback callback) {
		this.callback = callback;
	}

	@Override
	public void send(ByteBuffer buf) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(byte[] bytes, int offset, int length) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void leave() {
		// TODO Auto-generated method stub
		
	}

}
