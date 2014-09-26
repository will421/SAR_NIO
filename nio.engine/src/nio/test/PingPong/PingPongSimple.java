package nio.test.PingPong;

import java.io.IOException;
import java.net.InetAddress;

import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;
import nio.implementation1.CNioEngine;


	

public class PingPongSimple {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Thread(new PingPongServer(4211)).start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		new Thread(new PingPongClient("localhost",4211)).start();
	}

}


