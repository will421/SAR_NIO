package nio.multicast.implementation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
		NOTHINGNESS,
		CONNECT_TO_SERVER,
		CONNECT_TO_MEMBER,
		WORKING
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

	public MulticastEngine() throws Exception {
		nEngine = new CNioEngine();
		channelServer = null;
		callback = null;
		this.mPid = -1;
		state = ENGINE_STATE.NOTHINGNESS;
		//members = new HashMap<Integer,NioChannel>();
		members = null;
		myClock = 0;
		unknowChannels = new LinkedList<NioChannel>();
		queue = new LinkedList<MulticastQueueElement>();
	}

	@Override
	public String toString() {
		return "{"+mPid+"}";
	}

	@Override
	public void join(String adr, int port, IMulticastCallback callback) {
		if(state == ENGINE_STATE.NOTHINGNESS)
		{
			state = ENGINE_STATE.CONNECT_TO_SERVER;
			try {
				nEngine.connect(InetAddress.getByName(adr), port, this);
			} catch (SecurityException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.callback = callback;
		}
		else
		{
			try {
				throw new Exception("Pas possible d'appeler deux fois join");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
		System.out.println("{"+mPid+"}"+"Send M("+myClock+","+mPid+") x"+members.channels.length);
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
			buf.putInt(MESSAGE_SERVER_TYPE.BINDED.ordinal());
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
		} else if(type==MESSAGE_SERVER_TYPE.BEGIN)
		{
			callback.joined(this, mPid);
			state = ENGINE_STATE.WORKING;
			//TODO temp
			//String s = "ahaha from "+"{"+mPid+"}";
			//this.send(s.getBytes(), 0, s.getBytes().length);
		}
		else
		{
			try {
				throw new Exception("Should not occur");
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		else
		{
			try {
				throw new Exception("Should not occur");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void handleReceiveMessage(ByteBuffer bytes)
	{

		bytes.position(0);
		bytes.getInt();
		long clock = bytes.getLong();
		int pidM = bytes.getInt();

		//add to stack with clock
		MulticastQueueElement el = MulticastQueueElement.getElement(queue, clock, pidM);
		if(el==null)
		{
			el = new MulticastQueueElement(bytes, groupSize);
			queue.add(el);
			Collections.sort(queue);
		} else
		{
			el.updateMessage(bytes);
		}

		myClock = Math.max(myClock,el.getClock());

		//TODO REMOVE
		sendACK(bytes);
		//handleDeliver();
		//callback.deliver(this, bytes); //TODO A retirer
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
		//System.out.println("{"+mPid+"}:ACK:M("+clock+","+pidM+")/"+pidS);
		MulticastQueueElement el = MulticastQueueElement.getElement(queue, clock, pidM);
		if (el==null)
		{
			el = new MulticastQueueElement(clock,pidM,groupSize);
			queue.add(el);
			Collections.sort(queue);
		}
		el.ackReceived(pidS);

		//Est ce que l'on delivre ou non ?
		tryToDeliver();

	}


	private void tryToDeliver()
	{
		if(queue.isEmpty())
			return;
		MulticastQueueElement first= queue.get(0);
		System.out.println(this.toString()+"||"+Arrays.toString(queue.toArray()));
		boolean deliver = (members.getMask() & ~first.getAcksMask())==0 ;

		if(deliver){
			if(first.getType()== MESSAGE_TYPE.MESSAGE){
				callback.deliver(this, first.getMessage());
				//String s = new String(first.getMessage().array());
				//System.out.println("###{"+mPid+"}"+"deliver : M("+first.getClock()+","+first.getPid()+")"+"->"+s);
			}
			else if (first.getType() == MESSAGE_TYPE.ADD_MEMBER){
				//add member
				try {
					throw new Exception("Should not occur");
				} catch (Exception e) {
					e.printStackTrace();
				}

			} else if (first.getType() == MESSAGE_TYPE.NOT_RECEIVED_YET){
				return;
			}
			else{
				try {
					throw new Exception("Should not occur");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			queue.remove(0);
			tryToDeliver();
			/*
			if(queue.isEmpty())
				return;
			first = queue.get(0);
			deliver = (members.getMask() & ~first.getAcksMask())==0 ;*/

		}



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
			if(ch==null)
			{
				continue;
			}
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
			}
		}
		else
		{
			try {
				throw new Exception("Should not occur");
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		else
		{
			try {
				throw new Exception("Should not occur:new member ?");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		if(channel==channelServer)
		{
			handleReceiveServer(bytes);
		}
		else if(members.contains(channel) || members.channelInLocal == channel)
		{
			handleReceiveMember(bytes);
		} else if(unknowChannels.contains(channel))
		{
			MESSAGE_TYPE type = MESSAGE_TYPE.values()[bytes.getInt()];
			if(type == MESSAGE_TYPE.ID)
			{
				int id = bytes.getInt();
				if(id == mPid)
				{
					if(members.channelInLocal==null)
						members.channelInLocal = channel;
					else
						members.connected(mPid, channel);
				}
				else
				{
					members.connected(id, channel);
				}
				if(state==ENGINE_STATE.CONNECT_TO_MEMBER)
				{
					if(members.full())
					{
						ByteBuffer buff = ByteBuffer.allocate(4);
						buff.putInt(MESSAGE_SERVER_TYPE.READY.ordinal());
						channelServer.send(buff);
					}
				}
			}
			else
			{
				try {
					throw new Exception("Should not occur");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			unknowChannels.remove(channel);
		}
		else
		{
			try {
				throw new Exception("Should not occur");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public void closed(NioChannel channel) {
		//Retirer le membre de la liste interne
		members.disconnected(channel);
	}



}
