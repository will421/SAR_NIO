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

import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;
import nio.implementation1.CNioEngine;
import nio.implementation1.CNioServer;
import nio.multicast.IJoinedCallback;
import nio.multicast.IMulticastEngine;
import nio.multicast.IMulticastGroup;
import nio.multicast.IMulticastServer;

public class MulticastEngine implements IMulticastEngine,AcceptCallback,ConnectCallback,DeliverCallback {

	enum ENGINE_STATE{
		CONNECT_TO_SERVER,
		CONNECT_TO_MEMBER
	}
	
	
	private HashMap<Integer,NioChannel> members;
	private NioEngine nEngine;
	private IJoinedCallback joinCallback;
	private NioChannel channelServer;
	private IMulticastServer multicastServer;
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
		multicastServer = null;
		joinCallback = null;
		this.n = n;
		state = ENGINE_STATE.CONNECT_TO_SERVER;
		members = new HashMap<Integer,NioChannel>();
	}
	
	
	
	@Override
	public IMulticastServer join(String adr, int port, IJoinedCallback callback) {
		
		try {
			nEngine.connect(InetAddress.getByName(adr), port, this);
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		joinCallback = null;
		MulticastServer ms = new MulticastServer(adr,port);
		multicastServer = ms;
		return ms;
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
				IMulticastGroup group = new MulticastGroup(nEngine);
				joinCallback.joined(multicastServer, group);
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
			IMulticastGroup group = new MulticastGroup(nEngine);
			joinCallback.joined(multicastServer, group);
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
	
}
