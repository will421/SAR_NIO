package nio.engine;

public class PingPongSimple {

	static String prefServer = "[Server]";
	static String prefClient = "[Client]";
	
	public static void server (int port)
	{
		
		System.out.println(prefServer+"Server launched with port= "+port);
	}
	
	public static void client(String adr,int port)
	{
		System.out.println(prefClient+"Client launched, it will connect to "+adr+":"+port);
		
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Thread(new Runnable() {
			public void run() {
				server(4211);
			}
		}).run();
		
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
		}).run();
	}

}
