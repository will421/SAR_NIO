package nio.multicast.implementation;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
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
import nio.multicast.IMulticastCallback;
import nio.multicast.IMulticastEngine;

public class MulticastEngine implements IMulticastEngine,AcceptCallback,ConnectCallback,DeliverCallback {

	
	class MulticastQueueElement implements Comparable<MulticastQueueElement> {
		private MESSAGE_TYPE type;
		private ByteBuffer message;
		private long clock;
		private int pid;
		private byte[] acks;
		

		public MulticastQueueElement(ByteBuffer bytes,int groupSize) {
			bytes.position(0);
			type = MESSAGE_TYPE.values()[bytes.getInt()];
			clock = bytes.getLong();
			pid = bytes.getInt();
			message = bytes.slice();
			acks = new byte[groupSize];
			
		}
		
		public long getClock()
		{
			return clock;
		}
		@Override
		public int compareTo(MulticastQueueElement o) {
			MulticastQueueElement elm = (MulticastQueueElement)o;
			if(this.clock<elm.clock)
				return -1;
			else if(this.clock>elm.clock)
				return 1;
			else
			{
				if(this.pid<elm.pid)
					return -1;
				else if(this.pid>elm.pid)
					return 1;
				else
				{
					try {
						throw new Exception("Should not occur");
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(-1);
					}
					return 0;
				}
			}
		}
		 
	}
	
	
	enum ENGINE_STATE{
		CONNECT_TO_SERVER,
		CONNECT_TO_MEMBER,
		WORKING
	}
	
	
	enum MESSAGE_TYPE{
		MESSAGE,
		ACK,
		ADD_MEMBER,
	}
	
	
	private HashMap<Integer,NioChannel> members; //Membres du groupes
	private NioEngine nEngine;	//NioEngine utilisé
	private IMulticastCallback callback; //Objet recevant les sorties du multicast engine
	private NioChannel channelServer; //Channel de communication avec le serveur de groupe
	private NioServer connectChannel;  //Port de connection à ce membre
	private int n ; //Numero du membre dans la liste
	private int lastConnected; //Dernier membre auquel on s'est connecté (initialisation)
	private ENGINE_STATE state; //Etat de l'engine
	private long myClock;
	private int groupSize;
	private List<MulticastQueueElement> queue;
	
	private String adrGroup[]; //Representation interne de la liste
	private int ports[]; //Representation interne de la liste
	
	
	public MulticastEngine(int n) throws Exception {
		nEngine = new CNioEngine();
		channelServer = null;
		callback = null;
		this.n = n;
		state = ENGINE_STATE.CONNECT_TO_SERVER;
		members = new HashMap<Integer,NioChannel>();
		myClock = 0;
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


	
	private void receptionList(NioChannel channel,String[] ips,int[] ports,int n)
	{
		adrGroup = ips.clone();
		this.ports = ports.clone();
		groupSize = adrGroup.length;
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
		
		lastConnected = n;
		try {
			nEngine.connect(InetAddress.getByName(adrGroup[lastConnected]), ports[lastConnected],this);
		} catch (SecurityException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		state=ENGINE_STATE.CONNECT_TO_MEMBER;
	}


	@Override
	public void send(ByteBuffer buf) {
		ByteBuffer bytes = ByteBuffer.allocate(buf.capacity()+4+8+4);
		bytes.putInt(MESSAGE_TYPE.MESSAGE.ordinal());
		bytes.putLong(myClock);
		bytes.putInt(n);
		buf.position(0);
		bytes.put(buf);
		for(Entry<Integer,NioChannel> entry : members.entrySet()) {
		    //Integer key = entry.getKey();
		    NioChannel value = entry.getValue();
		    value.send(bytes);
		}
		myClock++;
	}

	@Override
	public void send(byte[] bytes, int offset, int length) {
		ByteBuffer buffer = ByteBuffer.allocate(length);
		byte[] copy = bytes.clone();
		
		for(int i =offset;i<offset+length;i++)
		{
			buffer.put(copy[i]);
		}
		this.send(buffer);
	}


	@Override
	public void leave() {
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
	
	private void isJoined()
	{
		if(state==ENGINE_STATE.CONNECT_TO_MEMBER)
		{
			if(members.size()==adrGroup.length)
			{
				callback.joined(this);
			}
			state = ENGINE_STATE.WORKING;
		}
	}
	
	private void handleReceive(ByteBuffer bytes)
	{
		bytes.position(0);
		int type = bytes.getInt();
		//long clock = bytes.getLong();
		//int pid = bytes.getInt();
		if(type==MESSAGE_TYPE.MESSAGE.ordinal() || type==MESSAGE_TYPE.ADD_MEMBER.ordinal())
		{
			handleReceiveMessage(bytes);
		} else if(type==MESSAGE_TYPE.ACK.ordinal())
		{
			handleReceiveACK(bytes);
		}
	}
	
	private void handleReceiveMessage(ByteBuffer bytes)
	{
		//add to stack with clock
		MulticastQueueElement el = new MulticastQueueElement(bytes, groupSize);
		queue.add(el);
		Collections.sort(queue);
		myClock = Long.max(myClock,el.getClock());
		sendACK(bytes);
		callback.deliver(this, bytes); //TODO A retirer
	}
	
	private void handleReceiveACK(ByteBuffer bytes)
	{ 
		//tester l'ack et delivrer si suffisement d'ack
		/*
		 * format :
		 * 1) type message
		 * 2) clock message
		 * 3) pid message
		 * 4) pid envoyeur
		 */
		//TODO RECEPTION ACK
	}
	
	private void sendACK(ByteBuffer buf)
	{
		buf.position(0);
		int type = buf.getInt();
		long clock = buf.getLong();
		int pid = buf.getInt();
		
		
		ByteBuffer bytes = ByteBuffer.allocate(4+8+4+4);
		bytes.putInt(MESSAGE_TYPE.ACK.ordinal());
		bytes.putLong(clock);
		bytes.putInt(pid);
		bytes.putInt(n);

		for(Entry<Integer,NioChannel> entry : members.entrySet()) {
		    //Integer key = entry.getKey();
		    NioChannel value = entry.getValue();
		    value.send(bytes);
		}
		myClock++;
	}
	
	
	
	//callbacks from NioEngine
	@Override
	public void accepted(NioServer server, NioChannel channel) {
		if(connectChannel == server) //Si l'on cherche a se connecter à mon port de connexion dedié au groupe
		{
			int id = getId(channel.getRemoteAddress());
			if(id == -1) //Ca signifie que c'est un nouveau membre
				return;
			members.put(id, channel);

			isJoined();
		}
	}



	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		if(channel==channelServer)
			receptionList(channel,Option.ips,Option.ports,n);
		else if(members.containsValue(channel))
		{
			handleReceive(bytes);
			
		}
	}
	
	
	@Override
	public void closed(NioChannel channel) {
		// TODO Auto-generated method stub
		
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
			
			isJoined();
		}
		
	}
	
	
}
