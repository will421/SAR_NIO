package nio.test.PingPong_V2;

import java.io.IOException;

import util.string.*;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Random;

import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.Options;
import nio.implementation1.CNioEngine;

public class PingPongClient_V2 implements Runnable,ConnectCallback,DeliverCallback
{
	
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

		String msg = randomString.rdmString(Options.LG_MESSAGE_CLIENT);		
		channel.send(msg.getBytes(),0,msg.getBytes().length);


	}

	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		
		System.out.println(prefClient+"Message recu :"+ new String(bytes.array()));
		
		String msg = randomString.rdmString(Options.LG_MESSAGE_CLIENT);
		
		System.out.println(prefClient+"Message envoyé : "+ msg );
		
		channel.send(msg.getBytes(),0,msg.getBytes().length);
		
	}

	
	
}

