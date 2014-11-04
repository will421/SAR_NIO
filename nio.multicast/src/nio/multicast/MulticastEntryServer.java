package nio.multicast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import nio.engine.AcceptCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;

import nio.implementation1.CNioEngine;
import nio.multicast.implementation.MESSAGE_SERVER_TYPE;
import nio.multicast.implementation.Option;

public class MulticastEntryServer implements Runnable,AcceptCallback,DeliverCallback {

	private final int initialPort = 30000;
	private int lastPort = initialPort;

	private String _adr;
	private int _port;
	private int _nbMember;
	private int _nbMemberLeft;
	private NioChannel members[];
	private int ports[];
	private String adrs[];
	private int indice;
	private NioEngine engine;
	private HashMap<NioChannel,Integer> hmPorts;
	private List<NioChannel> ready;


	public MulticastEntryServer(String adr,int port, int nbMember) throws Exception {
		_adr = adr;
		_port = port;
		_nbMember = nbMember;
		_nbMemberLeft = nbMember;
		engine = new CNioEngine();
		
		members = new NioChannel[nbMember];
		adrs = new String[nbMember];
		ports = new int[nbMember];
		
		hmPorts = new HashMap<NioChannel,Integer>();
		indice = 0;
		ready = new LinkedList<NioChannel>();
	}


	@Override
	public void run() {
		try {
			engine.listen(_port, this);
			engine.mainloop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void accepted(NioServer server, NioChannel channel) {
		System.out.println("[Server]On accepte");
		channel.setDeliverCallback(this);

		//members.add(channel);
		sendPort(channel);

	}

	private void sendPort(NioChannel channel)
	{
		ByteBuffer buffer = ByteBuffer.allocate(4+4);
		int port = lastPort;
		buffer.putInt(MESSAGE_SERVER_TYPE.PORT.ordinal());
		buffer.putInt(port);
		channel.send(buffer);
		
		hmPorts.put(channel, port);
		lastPort++;
	}



	@Override
	public void closed(NioChannel channel) {
		// TODO Auto-generated method stub

	}


	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		// TODO Auto-generated method stub
		bytes.position(0);
		MESSAGE_SERVER_TYPE type = MESSAGE_SERVER_TYPE.values()[bytes.getInt()];
		//System.out.println("[Server]Type:"+type.toString()+" delivered");
		if(type == MESSAGE_SERVER_TYPE.BINDED)
		{
			_nbMemberLeft--;
			members[indice] = channel;
			adrs[indice]=channel.getRemoteAddress().getHostString();
			ports[indice] = hmPorts.get(channel);
			indice++;
			if(_nbMemberLeft==0)
			{
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ObjectOutput out = null;
				try {
					out = new ObjectOutputStream(bos);   
					out.writeObject(adrs);
					out.writeObject(ports);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				byte[] byteArray = bos.toByteArray();
				ByteBuffer buffer = ByteBuffer.allocate(4+4+4+byteArray.length);
				for(int id=0;id<_nbMember;id++)
				{
					buffer = ByteBuffer.allocate(4+4+4+byteArray.length);
					buffer.position(0);
					buffer.putInt(MESSAGE_SERVER_TYPE.LIST.ordinal());
					buffer.putInt(id); //Le pid du membre
					buffer.putInt(byteArray.length);
					buffer.put(byteArray);
					members[id].send(buffer);
				}
				//System.out.println("[Server]List sended");
			}
		}
		else if(type==MESSAGE_SERVER_TYPE.NEW_PORT)
		{
			this.sendPort(channel);
		}
		else if(type==MESSAGE_SERVER_TYPE.READY)
		{
			boolean allReady = true;
			ready.add(channel);
			for(int id=0;id<_nbMember;id++)
			{
				if(!ready.contains(members[id]))
				{
					allReady =false;
					break;
				}
			}
			if(allReady)
			{
				ByteBuffer buf = ByteBuffer.allocate(4);
				buf.putInt(MESSAGE_SERVER_TYPE.BEGIN.ordinal());
				for(NioChannel ch : members)
					ch.send(buf);
			}
		}
	}

	public static void main(String args[]){

		try {
			new MulticastEntryServer("localhost", 8888, Integer.parseInt(args[0]));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



	}
}