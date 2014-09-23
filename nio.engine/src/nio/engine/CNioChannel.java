package nio.engine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.LinkedList;



public class CNioChannel extends NioChannel /*implements AcceptCallback*/ {


	private ByteBuffer buffer_out;
	
	
	private ByteBuffer buffer_length = ByteBuffer.allocate(4);
	private ByteBuffer buffer_read = null;

	private DeliverCallback callback;
	private SocketChannel socketChannel;
	private CNioEngine nEngine;


	// Declaration of Automaton state for the read-auomaton
	static final int READING_LENGTH = 1; 
	static final int READING_MSG = 2;

	int currentState = READING_LENGTH; // initial state 

	



	public CNioChannel(SocketChannel socketChannel,CNioEngine nEngine) {
		this.socketChannel = socketChannel;
		this.nEngine = nEngine;
	}

	@Override
	public SocketChannel getChannel() {
		return socketChannel;
	}

	@Override
	public void setDeliverCallback(DeliverCallback callback) {
		this.callback = callback;

	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void send(ByteBuffer buf) {

		buffer_out = buf.duplicate();
		nEngine.wantToWrite(this);
	}

	@Override
	public void send(byte[] bytes, int offset, int length) {

		while (length != 0 ){


			this.buffer_out.put(bytes[length]);

			offset++;
			length--;
		}

		nEngine.wantToWrite(this);

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	public void readAutomaton() throws IOException 
	{
		//Penser à peut etre boucler un peu au cas où le buffer NIO contienne plusieurs messages
		
		

	}

	
	
	
	/*
	@Override
	public void accepted(NioServer server, NioChannel channel) {
		String message;
		message = "Connexion acceptée sur le serveur. Port : " + Integer.toString(server.getPort());
		System.out.println(message);
	}

	@Override
	public void closed(NioChannel channel) {

		//TODO Préciser le nom/num de channel
		String message;
		message = "Channel Fermée";
		System.out.println(message);
	}
	 */
}
