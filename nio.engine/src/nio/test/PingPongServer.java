package nio.test;

import java.io.IOException;

import nio.engine.AcceptCallback;
import nio.engine.CNioEngine;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;

public class PingPongServer implements AcceptCallback,Runnable
{
	static final String prefServer = "[Server]";
	int port;


	public PingPongServer(int p) {
		port = p;
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
<<<<<<< HEAD
=======
	
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


>>>>>>> parent of 19db53e... Ping pong pret, plus qu'à faire marcher


}