package nio.test.PingPong_V1;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.implementation1.CNioEngine;

public class PingPongClient implements Runnable,ConnectCallback,DeliverCallback
{
	static final String prefClient = "[Client]";

	NioChannel clientChannel = null;
	String adr;
	int port;
	int n;

	public PingPongClient(String s, int p) {
		adr = s;
		port = p;
		n = 1;
	}

	@Override
	public void run() {
		NioChannel nChannel = null;
		System.out.println(prefClient+"Client launched, it will connect to "+adr+":"+port);
		NioEngine engine = null;
		try {
			engine = new CNioEngine();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
		try {
			engine.connect(InetAddress.getByName(adr), port, this);


		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		engine.mainloop();

	}

	@Override
	public void closed(NioChannel channel) {
		System.out.println(prefClient+"ConnectCallback closed");

	}

	@Override
	public void connected(NioChannel channel) {
		System.out.println(prefClient+"ConnectCallback connected");
		clientChannel = channel;
		clientChannel.setDeliverCallback(this);
		String ping = "Ping"+n;
		n++;
		channel.send(ping.getBytes(),0,ping.getBytes().length);
		//ByteBuffer buf =  ByteBuffer.allocate(ping.getBytes().length);
		//buf.put(ping.getBytes());
		//channel.send(buf);

	}

	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		//System.out.println(prefClient+"Message recu :"+ new String(bytes.array()));
		String ping = "Ping"+n;
		n++;
		channel.send(ping.getBytes(),0,ping.getBytes().length);
	}

	
	
	
}

