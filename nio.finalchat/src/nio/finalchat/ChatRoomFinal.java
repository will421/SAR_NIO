package nio.finalchat;



import nio.multicast.IMulticastEngine;
import chat.gui.ChatException;
import chat.gui.ChatGUI;
import chat.gui.IChatRoom;


public class ChatRoomFinal implements IChatRoom, Runnable {

	EventPump m_pump;
	String _adr;
	int _port;
	String _topic;
	
	IMulticastEngine engine;
	
	
	ChatRoomFinal(String topic,String adr, int port) {
		
		this.m_pump = new EventPump(this);
		this.m_pump.start();
		this._topic=topic;
		this._adr=adr;
		this._port=port;
	}

	@Override
	public void enter(String clientName, IChatListener l) throws ChatException {
		 // TODO
		//engine.join(_adr,_port, callback);
		

	}

	@Override
	public void leave() throws ChatException {
		// TODO Auto-generated method stub

	}

	@Override
	public void send(String msg) throws ChatException {
		// TODO Auto-generated method stub

	}

	@Override
	public void run() {
		
	    ChatRoomFinal room = this;
	    
	    new ChatGUI("Salle de Chat", room);
	  
	}
	


}
