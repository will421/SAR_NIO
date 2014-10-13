package nio.test.PingPong_Close;

import java.io.IOException;
import java.net.InetAddress;

import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;
import nio.implementation1.CNioEngine;


public class PingPong_close{

	
	static public final int NB_CLIENTS = 20;
	static public final int NB_MESSAGE = 1;
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stu
		BreakdownSimulator bds = new BreakdownSimulator();
		Thread t;
		
		t= new Thread(new PingPongServer_close(4211));
		t.start();
		bds.add(t);

		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}

	
		for  (int i= 1;i<= PingPong_close.NB_CLIENTS;i++){
			t= new Thread(new PingPongClient_close("localhost",4211,i));
			t.start();
			bds.add(t);
		}
		
		new Thread(bds).start();
		
	}

}


