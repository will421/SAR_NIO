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
		
	

		if (currentState == READING_LENGTH) { // Lecture de la taille totale du message 

			int length_msg = socketChannel.read(buffer_length);

			buffer_read = ByteBuffer.allocate(length_msg);
			
			if(buffer_read.position()==3){
				buffer_read.position(0); // on se reposition au d�but pour ecraser et r��crer dessus
				currentState = READING_MSG;
			}
		}

		else if ( currentState == READING_MSG ){
			
			// routine lecture message
			//....			
		    //....		
			
			
			
			if( buffer_read.remaining() == 0 ) {
				//lecture compl�te on envoie

				//callback
				callback.deliver(this, buffer_read.duplicate()); //duplicate car le buffer est ecras� pour la prochaine reception, l'utilisateur peut perdre son message

				// On vide le buffer apr�s envoi 
				buffer_read = null;	
				currentState = READING_LENGTH;
				
			}

			else { // Message non recu correctement

				System.err.println("Message non recu correctement");
				currentState = READING_LENGTH;
			}

		}

	}

	
	
	
	/*
	@Override
	public void accepted(NioServer server, NioChannel channel) {
		String message;
		message = "Connexion accept�e sur le serveur. Port : " + Integer.toString(server.getPort());
		System.out.println(message);
	}

	@Override
	public void closed(NioChannel channel) {

		//TODO Pr�ciser le nom/num de channel
		String message;
		message = "Channel Ferm�e";
		System.out.println(message);
	}
	 */
}
