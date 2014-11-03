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

	int _idClient; //pour le PID

	String _adr;
	int _port;

	IChatListener m_listener;
	IMulticastEngine engine;


	ChatRoomFinal(String adr, int port) throws Exception {

		this.m_pump = new EventPump(this);
		this.m_pump.start();
		//this._idClient = Integer.parseInt(clientName.replaceAll("[^\\d.]", ""));
		//this._clientName= clientName;
		this._adr=adr;
		this._port=port;
		this._idClient =-1;
		this.engine = new MulticastEngine();  

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
	
	}

	@Override
	public void send(String msg) throws ChatException {

		final byte[] msgbb;

		msgbb= msg.getBytes();

		m_pump.enqueue(new Runnable() {
			public void run() {
				engine.send(msgbb,0, msgbb.length);;
			};
		});
	}

	@Override
	public void run() {

		ChatRoomFinal room = this;
		
		String name = "" + _idClient;
		new ChatGUI(name, room,false);

	}

	@Override
	public void deliver(IMulticastEngine engine, ByteBuffer bytes) {
		
    //	engine.send( bytes.ge), 0 , bytes.limit() );

	}


	@Override
	public void joined(IMulticastEngine engine, int pid) {
		_idClient=pid;
	}

   public static void main(String args[]){
	   
	   try {
		new ChatRoomFinal(args[0], Integer.parseInt(args[1])).run();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	   
	   
   }


}
