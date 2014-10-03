package nio.implementation1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.LinkedList;

import nio.engine.DeliverCallback;
import nio.engine.NioChannel;



public class CNioChannel extends NioChannel /*implements AcceptCallback*/ {

	private static final int isAMessage = 0x1;
	private static final int containChecksum = 0x2;
	
	private static final int outMetaData = isAMessage | containChecksum;
	
	
	private LinkedList<ByteBuffer> buffers_out;
	private ByteBuffer currentBufferOut;
	private ByteBuffer outBufferLength;
	private ByteBuffer outBufferMetaData;
	private ByteBuffer outBufferChecksum;
	
	
	
	private ByteBuffer buffer_length;
	private ByteBuffer buffer_read = null;
	private ByteBuffer inBufferMetaData;
	private int inMetadata;
	private ByteBuffer inBufferChecksum;

	private DeliverCallback callback;
	private SocketChannel socketChannel;
	private CNioEngine nEngine;


	// Declaration of Automaton state for the read-auomaton
	enum READING_STATE {
		READING_DONE,
		READING_METADATA,
		READING_LENGTH,
		READING_MSG,
		READING_CHECKSUM
	}
	enum SENDING_STATE {
		SENDING_DONE,
		SENDING_METADATA,
		SENDING_LENGTH,
		SENDING_MSG,
		SENDING_CHECKSUM
	}
	

	READING_STATE currentReadingState = READING_STATE.READING_DONE; 
	SENDING_STATE currentSendingState = SENDING_STATE.SENDING_DONE;
	



	public CNioChannel(SocketChannel socketChannel,CNioEngine nEngine) {
		this.socketChannel = socketChannel;
		this.nEngine = nEngine;
		this.buffers_out = new LinkedList<ByteBuffer>();
		buffer_length = ByteBuffer.allocate(4);
		outBufferLength = ByteBuffer.allocate(4);
		
		inBufferMetaData = ByteBuffer.allocate(4);
		outBufferMetaData = ByteBuffer.allocate(4);
		
		inBufferChecksum = ByteBuffer.allocate(16);
		outBufferChecksum = ByteBuffer.allocate(16);
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

		//ByteBuffer temp = buf.duplicate();
		buffers_out.add(buf);
		nEngine.wantToWrite(this);
	}

	@Override
	public void send(byte[] bytes, int offset, int length) {

		ByteBuffer buffer = ByteBuffer.allocate(length);
		
		for(int i =offset;i<offset+length;i++)
		{
			buffer.put(bytes[i]);
		}

		send(buffer);

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	public void readAutomaton()
	{
		//Penser à boucler un peu au cas où le buffer NIO contienne plusieurs messages
		
		if(currentReadingState == READING_STATE.READING_DONE)//reinit buffers
		{
			buffer_read = null;	
			buffer_length.position(0); // on se reposition au début pour ecraser et réécrire dessus
			inBufferMetaData.position(0);
			currentReadingState = READING_STATE.READING_METADATA;
		}
		if(currentReadingState == READING_STATE.READING_METADATA)
		{
			try {
				socketChannel.read(inBufferMetaData);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			if(inBufferMetaData.remaining()==0){
				inBufferMetaData.position(0);
				inMetadata = inBufferMetaData.getInt();
				currentReadingState = READING_STATE.READING_LENGTH;
			}
		}
		
		
		if (currentReadingState == READING_STATE.READING_LENGTH) { // Lecture de la taille totale du message 
			try {
				socketChannel.read(buffer_length);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			if(buffer_length.remaining()==0){
				buffer_length.position(0);
				int length_msg = buffer_length.getInt();
				buffer_read = ByteBuffer.allocate(length_msg);
				currentReadingState = READING_STATE.READING_MSG;
			}
		}

		if ( currentReadingState == READING_STATE.READING_MSG ){
			// routine lecture message
			try {
				socketChannel.read(buffer_read);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			if( buffer_read.remaining() == 0 ) {
				//lecture complète on envoie
				//callbacke

				currentReadingState = READING_STATE.READING_CHECKSUM;
			}
		}
		if( currentReadingState == READING_STATE.READING_CHECKSUM)
		{
			if( (inMetadata & containChecksum)!=0)
			{
				
				try {
					socketChannel.read(inBufferChecksum);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				} 
				
				if(inBufferChecksum.remaining()==0)
				{
					try {
						byte[] checksum = createChecksum(buffer_read);
						if(!checkChecksum(inBufferChecksum, checksum))
						{
							throw new Exception("Checksum failed");
						}
							
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
						System.exit(1);
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(1);
					}
					currentReadingState = READING_STATE.READING_DONE;
				}		
			}
			else
			{
				currentReadingState = READING_STATE.READING_DONE;
			}
		}
		
		if (currentReadingState == READING_STATE.READING_DONE && (inMetadata & isAMessage)!=0 )
			callback.deliver(this, buffer_read.duplicate()); //duplicate car le buffer est ecrasé pour la prochaine reception, l'utilisateur peut perdre son messag
		
		
	}

	public boolean sendAutomatton() {
		//retourne true si il n'y a plus rien a envoyer
		//Penser à continuer l'envoi si il reste de la place dans le bufferNio
		if(currentSendingState == SENDING_STATE.SENDING_DONE)
		{
			currentBufferOut = buffers_out.pop();
			currentBufferOut.position(0);
			outBufferLength.position(0);
			outBufferLength.putInt(currentBufferOut.capacity());
			outBufferLength.position(0);
			
			outBufferMetaData.position(0);
			outBufferMetaData.putInt(outMetaData);
			outBufferMetaData.position(0);
			
			outBufferChecksum.position(0);
			
			currentSendingState = SENDING_STATE.SENDING_METADATA;
		}
		
		if(currentSendingState == SENDING_STATE.SENDING_METADATA)
		{
			try {
				socketChannel.write(outBufferMetaData);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			if(outBufferMetaData.remaining()==0){
				currentSendingState = SENDING_STATE.SENDING_LENGTH;
			}	
		}
		
		if (currentSendingState == SENDING_STATE.SENDING_LENGTH) { // Lecture de la taille totale du message 
			try {
				socketChannel.write(outBufferLength);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			if(outBufferLength.remaining()==0){
				currentSendingState = SENDING_STATE.SENDING_MSG;
			}
		}
		
		if(currentSendingState == SENDING_STATE.SENDING_MSG)
		{
			try {
				socketChannel.write(currentBufferOut);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			if( currentBufferOut.remaining() == 0 ) {
				currentSendingState = SENDING_STATE.SENDING_CHECKSUM;
			}
		}
		
		if (currentSendingState == SENDING_STATE.SENDING_CHECKSUM) {
			if ((outMetaData & containChecksum) != 0) {
				try {
					byte[] checksum = createChecksum(currentBufferOut);
					outBufferChecksum.put(checksum);
					outBufferChecksum.position(0);
					socketChannel.write(outBufferChecksum);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
					System.exit(1);
				}

				if (outBufferChecksum.remaining() == 0) {
					currentSendingState = SENDING_STATE.SENDING_DONE;
				}
			} else {
				currentSendingState = SENDING_STATE.SENDING_DONE;
			}

		}
		
		
		return buffers_out.size()==0 && currentSendingState==SENDING_STATE.SENDING_DONE;
	}
	
	
	private static byte[] createChecksum(ByteBuffer buffer) throws NoSuchAlgorithmException{
		MessageDigest complete = MessageDigest.getInstance("MD5");
		buffer.position(0);
		complete.update(buffer);
		buffer.position(0);
		return complete.digest();
	}
	
	private static boolean checkChecksum(ByteBuffer buffer,byte[] bytes)
	{
		buffer.position(0);
		byte[] bytes2 = buffer.array();
		
		return java.util.Arrays.equals(bytes,bytes2);
	}
	

	
}
