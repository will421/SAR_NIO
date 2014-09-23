package nio.engine;

import java.io.IOException;
import java.net.InetAddress;

public class PingPongSimple {

	static String prefServer = "[Server]";
	static String prefClient = "[Client]";
	
	public static void server (int port)
	{
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
			engine.listen(port, new AcceptCallback() {

				@Override
				public void closed(NioChannel channel) {
					System.out.println(prefServer+"AcceptCallback closed");

				}

				@Override
				public void accepted(NioServer server, NioChannel channel) {
					System.out.println(prefServer+"AcceptCallback accepted");

				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		engine.mainloop();
	}
	
	public static void client(String adr,int port)
	{
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
			engine.connect(InetAddress.getByName(adr), port, new ConnectCallback() {
				
				@Override
				public void connected(NioChannel channel) {
					System.out.println(prefClient+"ConnectCallback connected");
					
				}
				
				@Override
				public void closed(NioChannel channel) {
					System.out.println(prefClient+"ConnectCallback closed");
					
				}
			});
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		engine.mainloop();
		
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Thread(new Runnable() {
			public void run() {
				server(4211);
			}
		}).start();
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		new Thread(new Runnable() {
			public void run() {
				client("localhost",4211);
			}
		}).start();
	}

}
