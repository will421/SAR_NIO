package nio.test;

import java.io.IOException;
import java.nio.ByteBuffer;

import nio.engine.AcceptCallback;
import nio.engine.CNioEngine;
import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;

public class PingPongServer implements Runnable,AcceptCallback,DeliverCallback
{
	static final String prefServer = "[Server]";
	int port;


	public PingPongServer(int p) {
		port = p;
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

	}


	@Override
	public void deliver(NioChannel channel, ByteBuffer bytes) {
		System.out.println(prefServer+"Message recu:"+ bytes.toString());
		
	}




}