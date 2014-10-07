package nio.test.PingPong_V2;

import java.net.InetAddress;

import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;
import nio.engine.Options;
import nio.implementation1.CNioEngine;


public class PingPong_V2{

	
	
	public static void main(String[] args) {
		new Thread(new PingPongServer_V2(4211)).start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

		for  (int i= 1;i<= Options.NB_CLIENTS;i++){
			new Thread(new PingPongClient_V2("localhost",4211,i)).start();
		}
		
	}

}


