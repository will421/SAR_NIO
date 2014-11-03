package nio.finalchat;



import java.nio.ByteBuffer;

import nio.multicast.IMulticastCallback;
import nio.multicast.IMulticastEngine;
import nio.multicast.implementation.MulticastEngine;
import chat.gui.ChatException;
import chat.gui.ChatGUI;
import chat.gui.IChatRoom;
import chat.gui.IChatRoom.IChatListener;


public class ChatRoomFinal implements IChatRoom, Runnable, IMulticastCallback {

	EventPump m_pump;
	
	String _clientName;
	int _idClient; //pour le PID
	
	String _adr;
	int _port;
	
	IChatListener m_listener;
	IMulticastEngine engine;
	
	
	ChatRoomFinal(String clientName,String adr, int port) throws Exception {
		
		this.m_pump = new EventPump(this);
		this.m_pump.start();
		this._idClient = Integer.parseInt(clientName.replaceAll("[^\\d.]", ""));
		this._clientName= clientName;
		this._adr=adr;
		this._port=port;
		this.engine = new MulticastEngine(this._idClient); 
		
	}

	@Override
	public void enter(String clientName, IChatListener l) throws ChatException {
		
		final ChatRoomFinal cpChatRoom = this;
		
		m_pump.enqueue(new Runnable() {
			
			public void run() {
				engine.join(_adr,_port,cpChatRoom);
				engine.mainloop();				
			}
		});
		

	}

	@Override
	public void leave() throws ChatException {
		// TODO Auto-generated method stub

	}

	@Override
	public void send(String msg) throws ChatException {
		
	}

	@Override
	public void run() {
		
	    ChatRoomFinal room = this;
	    
	    new ChatGUI("[Chat"+String.valueOf(this._idClient)+"]Salle de Chat view : " + _clientName, room);
	  
	}

	@Override
	public void deliver(IMulticastEngine engine, ByteBuffer bytes) {
		System.out.println("[Chat"+String.valueOf(this._idClient)+"]On est dans le deliver");
		
	}

	@Override
	public void joined(IMulticastEngine engine) {
		System.out.println("[Chat"+String.valueOf(this._idClient)+"]On est dans le joined");
		
	}
	
	


}
