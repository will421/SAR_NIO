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
		// TODO Auto-generated method stub
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
		nEngine.wantToWrite(this, buffer_out);
	}

	@Override
	public void send(byte[] bytes, int offset, int length) {

		while (length != 0 ){


			this.buffer_out.put(bytes[length]);

			offset++;
			length--;
		}
		nEngine.wantToWrite(this, buffer_out);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	public void received() throws IOException 
	{
		int state = this.currentState;
	

		if (state == READING_LENGTH) { // Lecture de la taille totale du message 

			int length_msg = socketChannel.read(buffer_length);			

			buffer_read = ByteBuffer.allocate(length_msg);
			
			
			buffer_read.position(0); // on se reposition au début pour ecraser et réécrer dessus
			state = READING_MSG;
		}

		else if ( state == READING_MSG ){
			
			// routine lecture message
			//....			
		    //....			
			
			if( buffer_read.remaining() == 0 ) {
				//lecture complète on envoie

				send(buffer_read);

				// On vide le buffer après envoi 
				buffer_read = null;	
				
			}

			else { // Message non recu correctement

				System.err.println("Message non recu correctement");
			}

		}

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
