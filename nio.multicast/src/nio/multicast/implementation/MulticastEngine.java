package nio.multicast.implementation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
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

	
	
	
	
	enum ENGINE_STATE{
		CONNECT_TO_SERVER,
		CONNECT_TO_MEMBER,
		WORKING
	}
	
	
	enum MESSAGE_TYPE{
		MESSAGE,
		ACK,
		ADD_MEMBER,
		NOT_RECEIVED_YET,
		ID
	}
	
	
	private NioEngine nEngine;	//NioEngine utilisé
	private IMulticastCallback callback; //Objet recevant les sorties du multicast engine
	private NioChannel channelServer; //Channel de communication avec le serveur de groupe
	private NioServer connectChannel;  //Port de connection à ce membre
	private int mPid ; //Numero du membre dans la liste
	private ENGINE_STATE state; //Etat de l'engine
	private long myClock;
	private int groupSize;
	private List<MulticastQueueElement> queue;
	private List<NioChannel> unknowChannels;
	
	private ListMember members;
	//private HashMap<Integer,NioChannel> members; //Membres du groupes
	//private String adrGroup[]; //Representation interne de la liste
	//private int ports[]; //Representation interne de la liste
	
	
	public MulticastEngine() throws Exception {
		nEngine = new CNioEngine();
		channelServer = null;
		callback = null;
		this.mPid = -1;
		state = ENGINE_STATE.CONNECT_TO_SERVER;
		//members = new HashMap<Integer,NioChannel>();
		members = null;
		myClock = 0;
		unknowChannels = new LinkedList<NioChannel>();
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
	public List<Integer> getPIDS() {
		return members.getPIDS();
	}
	
	
	private void receptionList(String[] ips,int[] ports)
	{
		groupSize = ips.length;
		members = new ListMember(groupSize);

		for(int i=0;i<groupSize;i++)
		{
			members.addMember(i, ips[i], ports[i]);
		}
			
		for(int i=mPid;i<groupSize;i++)
		{
			try {
				nEngine.connect(InetAddress.getByName(members.getAdr(i)), members.getPort(i),this);
				System.out.println("{"+mPid+"} connect to {"+i+"}");
			} catch (SecurityException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		state=ENGINE_STATE.CONNECT_TO_MEMBER;
	}


	@Override
	public void send(ByteBuffer buf) {
		ByteBuffer bytes = ByteBuffer.allocate(buf.capacity()+4+8+4);
		bytes.putInt(MESSAGE_TYPE.MESSAGE.ordinal());
		bytes.putLong(myClock);
		bytes.putInt(mPid);
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
				callback.joined(this,this.mPid);
				state = ENGINE_STATE.WORKING;
			}
			
		}

	}
	
	private void handleReceiveServer(ByteBuffer bytes) {
		// TODO Auto-generated method stub
		bytes.position(0);
		MESSAGE_SERVER_TYPE type = MESSAGE_SERVER_TYPE.values()[bytes.getInt()];
		System.out.println(String.valueOf("{"+mPid+"}"+": Receive from server:"+type.toString()));
		if(type==MESSAGE_SERVER_TYPE.PORT)
		{
			int port = bytes.getInt();
			try {
				connectChannel =  nEngine.listen(port, this);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			ByteBuffer buf = ByteBuffer.allocate(4);
			buf.putInt(MESSAGE_SERVER_TYPE.READY.ordinal());
			int i =MESSAGE_SERVER_TYPE.READY.ordinal(); 
			this.channelServer.send(buf);
		}
		else if(type == MESSAGE_SERVER_TYPE.LIST)
		{
			String[] ips = null;
			int[] ports = null;
			mPid = bytes.getInt();
			int size = bytes.getInt();
			byte[] byteArray = new byte[size];
			bytes.get(byteArray);

			ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
			ObjectInput in = null;

			try {
				in = new ObjectInputStream(bis);
				ips = (String[]) in.readObject();
				ports = (int[]) in.readObject();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			receptionList(ips,ports);
		}
		
	}
	
	private void handleReceiveMember(ByteBuffer bytes)
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
		bytes.putInt(mPid);

		for(NioChannel ch : members.channels) {
		    //Integer key = entry.getKey();
		    ch.send(bytes);
		}
		myClock++;
	}
	
	
	
	//callbacks from NioEngine
	@Override
	public void accepted(NioServer server, NioChannel channel) {
		if(state == ENGINE_STATE.CONNECT_TO_MEMBER)
		{
			if(connectChannel == server) //Si l'on cherche a se connecter à mon port de connexion dedié au groupe
			{
				unknowChannels.add(channel);
				channel.setDeliverCallback(this);
				ByteBuffer buf = ByteBuffer.allocate(4+4);
				buf.putInt(MESSAGE_TYPE.ID.ordinal());
				buf.putInt(mPid);
				channel.send(buf);
				/*
				members.connected(id, channel);
				channel.setDeliverCallback(this);
				isJoined();*/
			}
		}
		else
		{
			// new Member ?
		}
	}

	
	@Override
	public void connected(NioChannel channel) {
		
		if(state == ENGINE_STATE.CONNECT_TO_SERVER)
		{
			//On considere que le channel represente le serveurs
			channelServer = channel;
			channel.setDeliverCallback(this);

		} else if (state == ENGINE_STATE.CONNECT_TO_MEMBER)
		{
			unknowChannels.add(channel);
			channel.setDeliverCallback(this);
			ByteBuffer buf = ByteBuffer.allocate(4+4);
			buf.putInt(MESSAGE_TYPE.ID.ordinal());
			buf.putInt(mPid);
			channel.send(buf);
		}
	}

	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		if(channel==channelServer)
		{
			handleReceiveServer(bytes);
		}
		else if(members.contains(channel))
		{
			handleReceiveMember(bytes);
		} else if(unknowChannels.contains(channel))
		{
			MESSAGE_TYPE type = MESSAGE_TYPE.values()[bytes.getInt()];
			if(type == MESSAGE_TYPE.ID)
			{
				int id = bytes.getInt();
				members.connected(id, channel);
				isJoined();
			}
		}
	}


	@Override
	public void closed(NioChannel channel) {
		//Retirer le membre de la liste interne
		members.disconnected(channel);
	}


	
}
