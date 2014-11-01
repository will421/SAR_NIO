package nio.multicast.implementation;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;
import nio.implementation1.CNioEngine;
import nio.implementation1.CNioServer;
import nio.multicast.IMulticastCallback;
import nio.multicast.IMulticastEngine;

public class MulticastEngine implements IMulticastEngine,AcceptCallback,ConnectCallback,DeliverCallback {

	enum ENGINE_STATE{
		CONNECT_TO_SERVER,
		CONNECT_TO_MEMBER,
		WORKING
	}
	
	
	private HashMap<Integer,NioChannel> members;
	private NioEngine nEngine;
	private IMulticastCallback callback;
	private NioChannel channelServer;
	private NioServer connectChannel;
	//private MulticastGroup group;
	private int n ;
	private int lastConnected;
	private ENGINE_STATE state;
	
	private String adrGroup[];
	private int ports[];
	
	
	public MulticastEngine(int n) throws Exception {
		nEngine = new CNioEngine();
		channelServer = null;
		callback = null;
		this.n = n;
		state = ENGINE_STATE.CONNECT_TO_SERVER;
		members = new HashMap<Integer,NioChannel>();
	}
	
	
	
	@Override
	public void join(String adr, int port, IMulticastCallback callback) {
		
		try {
			nEngine.connect(InetAddress.getByName(adr), port, this);
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.callback = callback;

	}



	@Override
	public void mainloop() {
		nEngine.mainloop();
	}



	@Override
	public void connected(NioChannel channel) {
		
		if(state == ENGINE_STATE.CONNECT_TO_SERVER)
		{
			//On considere que le channel represente le serveurs
			channelServer = channel;
			//On conidere recevoir la liste
			this.deliver(channel, null);
			
		} else if (state == ENGINE_STATE.CONNECT_TO_MEMBER)
		{
			members.put(lastConnected, channel);
			if(lastConnected+1<adrGroup.length)
			{
				lastConnected++;
				try {
					nEngine.connect(InetAddress.getByName(adrGroup[lastConnected]), ports[lastConnected],this);
				} catch (SecurityException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(members.size()==adrGroup.length)
			{
				callback.joined(this);
			}
		}
		
	}

	//joinCallback.joined(server, group);
	
	
	private void receptionList(NioChannel channel,String[] ips,int[] ports,int n)
	{
		adrGroup = ips.clone();
		this.ports = ports.clone();
		lastConnected = n-1;
		
		/*
		for(int i=n;i<Option.ips.length;i++)
		{
			try {
				nEngine.connect(InetAddress.getByName(Option.ips[i]), Option.ports[i],this);
			} catch (SecurityException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		try {
			connectChannel =  nEngine.listen(ports[n], this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		lastConnected++;
		try {
			nEngine.connect(InetAddress.getByName(adrGroup[lastConnected]), ports[lastConnected],this);
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		state=ENGINE_STATE.CONNECT_TO_MEMBER;
	}



	@Override
	public void accepted(NioServer server, NioChannel channel) {
		members.put(getId(channel.getRemoteAddress()), channel);
		
		if(members.size()==adrGroup.length)
		{
			callback.joined(this);
		}
		
	}



	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		if(channel==channelServer)
			receptionList(channel,Option.ips,Option.ports,n);
		
	}


	@Override
	public void closed(NioChannel channel) {
		// TODO Auto-generated method stub
		
	}

	private int getId(InetSocketAddress adr)
	{
		List<Integer> l = new LinkedList<>();
		int res = -1;
		
		for(int i =0;i<adrGroup.length;i++)
		{
			if(adr.getHostString().equals(adrGroup[i]))
			{
				l.add(i);
			}
		}
		
		for(int i =0;i<l.size();i++)
		{
			if(adr.getPort() == ports[l.get(i)])
			{
				res = l.get(i);
				break;
			}
		}
		
		
		return res;
	}



	@Override
	public void send(ByteBuffer buf) {

		for(Entry<Integer,NioChannel> entry : members.entrySet()) {
		    Integer key = entry.getKey();
		    NioChannel value = entry.getValue();
		    
		    value.send(buf);
		}
		
	}

	@Override
	public void send(byte[] bytes, int offset, int length) {
		byte[] cpy = bytes.clone();
		for(Entry<Integer,NioChannel> entry : members.entrySet()) {
		    Integer key = entry.getKey();
		    NioChannel value = entry.getValue();
		    
		    value.send(cpy, offset, length);
		}
		
	}

	@Override
	public void leave() {
		// TODO Auto-generated method stub
		
	}

	
}
