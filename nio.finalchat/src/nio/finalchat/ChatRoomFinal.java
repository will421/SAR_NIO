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
	ChatGUI _gui;
	
	int _idClient;
	int _port;
	String _adr;
	
	Boolean _autoJoinDebug;

	IChatListener m_listener;
	IMulticastEngine engine;


	ChatRoomFinal(String adr, int port,Boolean autoJoinDebug) throws Exception {

		this.m_pump = new EventPump(this);
		this.m_pump.start();
		//this._idClient = Integer.parseInt(clientName.replaceAll("[^\\d.]", ""));
		//this._clientName= clientName;
		this._adr=adr;
		this._port=port;
		this._idClient =-1; // = -1 toujours pas dans le groupe
		this.engine = new MulticastEngine();  
		this._autoJoinDebug=autoJoinDebug;

	}

	@Override
	public void enter(String clientName, IChatListener l) throws ChatException {

		final ChatRoomFinal cpChatRoom = this;

		m_listener = l;
		
		m_pump.enqueue(new Runnable() {

			public void run() {
				engine.join(_adr,_port,cpChatRoom);
				engine.mainloop();

			}
		});

	}

	@Override
	public void leave() throws ChatException {
		
		System.out.println("[CHAT] : Je leave");
		engine.leave();
	}

	@Override
	public void send(String msg) throws ChatException {
		
		final byte[] msgbb;

		
		msgbb= msg.getBytes();
		System.out.println("[CHAT] :" + msgbb);
		engine.send(msgbb,0, msgbb.length);
		engine.getSelector().wakeup();
		
	}

	@Override
	public void run() {

		ChatRoomFinal room = this;
		String name = "Non défini";
		_gui = new ChatGUI(name, room,room._autoJoinDebug);
		

	}

	@Override
	public void deliver(IMulticastEngine engine, ByteBuffer bytes) {
		
	    String msg = new String(bytes.array());
		
		m_listener.deliver(msg);
	}


	@Override
	public void joined(int pid) {
		_idClient=pid;
		_clientName = "Client " + _idClient;
		_gui.getFrame().setTitle(_clientName);
	}


	@Override
	public void memberJoin(int pid) {
		// TODO Auto-generated method stub
		System.out.println("JOIN");
	}

	@Override
	public void memberQuit(int pid) {
		// TODO Auto-generated method stub
		System.out.println("QUIT");
	}

	@Override
	public void disconnected() {
		// TODO Auto-generated method stub
		System.out.println("DISCONNECTED");
	}


}
