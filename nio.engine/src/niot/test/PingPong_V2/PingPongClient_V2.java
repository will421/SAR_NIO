package niot.test.PingPong_V2;

import java.io.IOException;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.implementation1.CNioEngine;

public class PingPongClient_V2 implements Runnable,ConnectCallback,DeliverCallback
{
	static public final int LG_MESSAGE = 4  ; // Nb carac
	static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static Random rnd = new Random();
	
	String prefClient = "";
	NioChannel clientChannel = null;
	String adr;
	int port;
	int n;

	public PingPongClient_V2(String s, int p,int i) {
		adr = s;
		port = p;
		n = 1;
		prefClient = "[Client "+ i +"]";
	}

	@Override
	public void run() {
		NioChannel nChannel = null;
		System.out.println(prefClient+"Client : launched, it will connect to "+adr+":"+port);
		NioEngine engine = null;
		try {
			engine = new CNioEngine();
		} catch (Exception e) {
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

		String msg = randomString(LG_MESSAGE);		
		n++;
		channel.send(msg.getBytes(),0,msg.getBytes().length);
		//ByteBuffer buf =  ByteBuffer.allocate(ping.getBytes().length);
		//buf.put(ping.getBytes());
		//channel.send(buf);

	}

	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		System.out.println(prefClient+"Message recu :"+ new String(bytes.array()));
		String ping = "Ping"+n;
		n++;
		channel.send(ping.getBytes(),0,ping.getBytes().length);
		
	}

	
	
	

	String randomString( int len ) 
	{
	   StringBuilder sb = new StringBuilder( len );
	   for( int i = 0; i < len; i++ ) 
	      sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
	   return sb.toString();
	}
	
	
	
	
	
	
}

