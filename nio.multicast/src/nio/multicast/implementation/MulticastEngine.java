package nio.multicast.implementation;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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

	
	class ListMember{
		public int length;
		
		private boolean valid[];
		private String adrs[];
		private int ports[];
		public NioChannel channels[]; //remettre priver
		
		
		public ListMember(int length) {
			this.length = length;
			adrs= new String[length];
			ports = new int[length];
			channels = new NioChannel[length];
			valid = new boolean[length];
			
			for(int i=0;i<length;i++)
			{
				adrs[i] = null;
				ports[i] = -1;
				channels[i] = null;
				valid[i] = false;
			}
			
		}
		
		public boolean isConnected(int pid)
		{
			return channels[pid]!=null;
		}
		
		public void connected(int pid,NioChannel channel)
		{
			channels[pid] = channel;
		}
		
		public void disconnected(int pid)
		{
			channels[pid] = null;
			valid[pid] = false;
			adrs[pid] = null;
			ports[pid] = -1;
		}
		
		public void addMember(int pid,String adr,int port)
		{
			adrs[pid] = adr;
			ports[pid] = port;
			valid[pid] = true;
		}
		
		public String getAdr(int pid)
		{
			return adrs[pid];
		}
		
		public int getPort(int pid)
		{
			return ports[pid];
		}
		public NioChannel getChannel(int pid)
		{
			return channels[pid];
		}

		public boolean full()
		{
			boolean b = true;
			for(NioChannel ch : channels)
			{
				if(ch==null)
				{
					b = false;
					break;
				}
			}
			return b;
		}
		
		public boolean contains(NioChannel channel)
		{
			return Arrays.asList(channels).contains(channel);
		}
		
		public byte[] getMask()
		{
			byte[] mask = new byte[length];
			for(int i =0;i<length;i++)
			{
				if(channels[i]==null)
					mask[i]=0;
				else
					mask[i] =1;
				
			}
			return mask;
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
		NOT_RECEIVED_YET
	}
	
	
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
	
	private ListMember members;
	//private HashMap<Integer,NioChannel> members; //Membres du groupes
	//private String adrGroup[]; //Representation interne de la liste
	//private int ports[]; //Representation interne de la liste
	
	
	public MulticastEngine(int n) throws Exception {
		nEngine = new CNioEngine();
		channelServer = null;
		callback = null;
		this.n = n;
		state = ENGINE_STATE.CONNECT_TO_SERVER;
		//members = new HashMap<Integer,NioChannel>();
		members = null;
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
		groupSize = ips.length;
		members = new ListMember(groupSize);

		for(int i=0;i<groupSize;i++)
		{
			members.addMember(i, ips[i], ports[i]);
		}
		
		
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
			nEngine.connect(InetAddress.getByName(members.getAdr(lastConnected)),members.getPort(lastConnected),this);
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
		for(NioChannel channel: members.channels) {
		    //Integer key = entry.getKey();
		    channel.send(bytes);
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
		
		String adr2 = adr.getHostString();
		if (adr2.equals("127.0.0.1"))
			adr2 = "localhost";
		
		for(int i =0;i<groupSize;i++)
		{
			if(adr2.equals(members.getAdr(i)))
			{
				l.add(i);
			}
		}
		
		for(int i =0;i<l.size();i++)
		{
			if(adr.getPort() == members.getPort(l.get(i)))
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
			if(members.full())
			{
				callback.joined(this);
				state = ENGINE_STATE.WORKING;
			}
			
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
	
		myClock = Math.max(myClock,el.getClock());
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
		bytes.position(0);
		int type = bytes.getInt();
		long clock = bytes.getLong();
		int pidM = bytes.getInt();
		int pidS = bytes.getInt();
		MulticastQueueElement el = MulticastQueueElement.getElement(queue, clock, pidM);
		if (el==null)
		{
			el = new MulticastQueueElement(clock,pidM,groupSize);
			queue.add(el);
			Collections.sort(queue);
		}
		el.ackReceived(pidS);
		
		//Est ce que l'on delivre ou non ?
		handleDeliver();
		
	}
	
	
	private void handleDeliver()
	{
		MulticastQueueElement first = queue.get(0);
		//TODO handleDeliver
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

		for(NioChannel ch : members.channels) {
		    //Integer key = entry.getKey();
		    ch.send(bytes);
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
			members.connected(id, channel);

			isJoined();
		}
	}



	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		if(channel==channelServer)
			receptionList(channel,Option.ips,Option.ports,n);
		else if(members.contains(channel))
		{
			handleReceive(bytes);
		}
	}
	
	
	@Override
	public void closed(NioChannel channel) {
		//Retirer le membre de la liste interne
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
			members.connected(lastConnected, channel);
			if(lastConnected+1<groupSize)
			{
				lastConnected++;
				try {
					nEngine.connect(InetAddress.getByName(members.getAdr(lastConnected)), members.getPort(lastConnected),this);
				}


				catch (SecurityException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}
			}
			
			
		}
		isJoined();
		
	}
	
	
}
