package nio.multicast;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import nio.engine.AcceptCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;
import nio.engine.NioServer;
import nio.implementation1.CNioEngine;

public class MulticastServer implements Runnable,AcceptCallback {

	
	private String _adr;
	private int _port;
	private int _nbMember;
	private int _nbMemberLeft;
	private List<NioChannel> members;
	private NioEngine engine;
	
	
	public MulticastServer(String adr,int port, int nbMember) throws Exception {
		_adr = adr;
		_port = port;
		_nbMember = nbMember;
		_nbMemberLeft = nbMember;
		engine = new CNioEngine();
		members = new LinkedList<NioChannel>();
	}
	
	
	
	
	@Override
	public void run() {
		try {
			engine.listen(_port, this);
			engine.mainloop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	@Override
	public void accepted(NioServer server, NioChannel channel) {
		_nbMemberLeft--;
		members.add(channel);
		if(_nbMemberLeft==0)
		{
			//Envoyer la liste à tout le monde
		}
	}




	@Override
	public void closed(NioChannel channel) {
		// TODO Auto-generated method stub
		
	}

}
