package niot.test.PingPong_V2;

import java.net.InetAddress;

import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;
import nio.implementation1.CNioEngine;


public class PingPong_V2{

	
	static public final int NB_CLIENTS = 1;
	static public final int NB_MESSAGE = 1;
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stu
		new Thread(new PingPongServer_V2(4211)).start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

	
		for  (int i= 1;i<= PingPong_V2.NB_CLIENTS;i++){
			new Thread(new PingPongClient_V2("localhost",4211,i)).start();
		}
		
	}

}


