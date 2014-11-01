package nio.multicast.implementation;

import nio.multicast.IMulticastServer;

public class MulticastServer implements IMulticastServer {

	private int port;
	private String adr;
	
	
	public MulticastServer(String adr, int port) {
		this.adr = adr;
		this.port = port;
	}
	
	@Override
	public int getPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
