package nio.finalchat;



import java.nio.ByteBuffer;
import java.util.Random;

import util.string.randomString;
import nio.engine.Options;
import nio.multicast.IMulticastCallback;
import nio.multicast.IMulticastEngine;
import nio.multicast.implementation.MulticastEngine;
import nio.multicast.implementation.Option;
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
		this._idClient = -1; // = -1 toujours pas dans le groupe
		this.engine = new MulticastEngine();  
		this._autoJoinDebug=autoJoinDebug;

	}

	private void goBurst() {
		
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				String prefClient = _clientName + " : ";

				// version avec nb de message qui marche

				for(int i=0;i<Option.nbMessage;i++){


					Random rand = new Random();

					int taille_random =rand.nextInt(Option.maxMessageLength);

					String random_msg =randomString.rdmString(taille_random);
					try {
						Thread.sleep(Option.burstSleep);
						send(prefClient + random_msg);
						//System.out.println(prefClient + random_msg);
					} catch (ChatException e) {
						e.printStackTrace();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		};
		Thread t = new Thread(r);
		t.start();

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

		engine.leave();
	}

	@Override
	public void send(String msg) throws ChatException {

		final byte[] msgbb;

		msgbb= msg.getBytes();

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
		
		String prefClient = _clientName + " : ";
		String msg = new String(bytes.array());

		if(msg.equals("goburst")){
			goBurst();
		}

		m_listener.deliver(prefClient + msg);
	}


	@Override
	public void joined(int pid) {
		_idClient=pid;
		_clientName = "Client " + _idClient;
		_gui.getFrame().setTitle(_clientName);
	}


	@Override
	public void memberJoin(int pid) {
		//_gui.updateGroup();
		System.out.println("On Update le group "+ _clientName);
	}

	@Override
	public void memberQuit(int pid) {
		//_gui.updateGroup();
		System.out.println("On Update le group du " + _clientName);
	}

	@Override
	public void disconnected() {


	}


}
