package nio.test.PingPong;

import java.io.IOException;
import java.nio.ByteBuffer;

import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;
import nio.implementation1.CNioEngine;

public class PingPongServer implements Runnable,AcceptCallback,DeliverCallback
{
	static final String prefServer = "[Server]";
	int port;
	int n;

	public PingPongServer(int p) {
		port = p;
		n=1;
	}

	
	@Override
	public void run() {
		System.out.println(prefServer+"Server launched with port= "+port);
		NioEngine engine = null;
		try {
			engine = new CNioEngine();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

		try {
			engine.listen(port,this);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}

		engine.mainloop();

	}	
	
	@Override
	public void closed(NioChannel channel) {
		System.out.println(prefServer+"AcceptCallback closed");

	}

	@Override
	public void accepted(NioServer server, NioChannel channel) {
		System.out.println(prefServer+"AcceptCallback accepted");
		channel.setDeliverCallback(this);
	}


	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		System.out.println(prefServer+"Message recu :"+ new String(bytes.array()));
		String ping = "Pong"+n;
		n++;
		channel.send(ping.getBytes(),0,ping.getBytes().length);
	}




}