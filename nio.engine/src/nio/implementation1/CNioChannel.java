package nio.implementation1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.DeliverCallback;
import nio.engine.NioChannel;
import nio.engine.NioEngine;

public class CNioChannel extends NioChannel {

	private static final int isAMessage = 0x1;
	private static final int containChecksum = 0x2;
	private static final int isAheartBeat = 0x4;

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

	private DeliverCallback dCallback;
	private ConnectCallback cCallback;
	private AcceptCallback aCallback;

	private Selector _selector;

	private SocketChannel socketChannel;
	private CNioEngine nEngine;

	private Heartbeat heartbeat;
	private boolean hb;

	// Declaration of Automaton state for the read-automaton
	enum READING_STATE {
		READING_DONE, READING_METADATA, READING_LENGTH, READING_MSG, READING_CHECKSUM
	}
	
	enum SENDING_STATE {
		SENDING_DONE, SENDING_METADATA, SENDING_LENGTH, SENDING_MSG, SENDING_CHECKSUM
	}

	READING_STATE currentReadingState = READING_STATE.READING_DONE;
	SENDING_STATE currentSendingState = SENDING_STATE.SENDING_DONE;

	public CNioChannel(SocketChannel socketChannel, CNioEngine nEngine,
			ConnectCallback callback) {
		this.socketChannel = socketChannel;
		this.nEngine = nEngine;
		cCallback = callback;

		initialize();
	}

	public CNioChannel(SocketChannel socketChannel, CNioEngine nEngine,
			AcceptCallback callback) {
		this.socketChannel = socketChannel;
		this.nEngine = nEngine;
		aCallback = callback;

		initialize();
	}

	private void initialize()
	{
		_selector = nEngine.selector;
		
		this.buffers_out = new LinkedList<ByteBuffer>();
		buffer_length = ByteBuffer.allocate(4);
		outBufferLength = ByteBuffer.allocate(4);

		inBufferMetaData = ByteBuffer.allocate(4);
		outBufferMetaData = ByteBuffer.allocate(4);

		inBufferChecksum = ByteBuffer.allocate(16);
		outBufferChecksum = ByteBuffer.allocate(16);
		
		
		final CNioChannel nc = this;
		heartbeat = new Heartbeat(new Runnable() {
			@Override
			public void run() {
				nc.sendHeartbeat();
			}
		}, 10000);
		heartbeat=null;
		//new Thread(heartbeat).run();
	}
	
	
	@Override
	public SocketChannel getChannel() {
		return socketChannel;
	}

	@Override
	public void setDeliverCallback(DeliverCallback callback) {
		this.dCallback = callback;

	}

	@Override
	public InetSocketAddress getRemoteAddress() {

		InetSocketAddress adr_t;

		adr_t = new InetSocketAddress(this.socketChannel.socket()
				.getInetAddress(), this.socketChannel.socket().getPort());

		return adr_t;
	}

	private void sendHeartbeat() {
		hb = true;
		nEngine.wantToWrite(this);
	}

	@Override
	public void send(ByteBuffer buf) {

		// ByteBuffer temp = buf.duplicate();
		buffers_out.add(buf);
		nEngine.wantToWrite(this);
	}

	@Override
	public void send(byte[] bytes, int offset, int length) {

		ByteBuffer buffer = ByteBuffer.allocate(length);
		byte[] copy = bytes.clone();

		for (int i = offset; i < offset + length; i++) {
			buffer.put(copy[i]);
		}

		send(buffer);

	}

	@Override
	public void close() {
		try {
			if(heartbeat!=null)
			{
				heartbeat.stop();
				heartbeat = null;
			}
			socketChannel.close();
		} catch (IOException e) {
			// nothing to do if it is closed
		}
		if (cCallback != null && aCallback == null) {
			cCallback.closed(this);
		} else if (cCallback == null && aCallback != null) {
			aCallback.closed(this);
		} else
			NioEngine.panic("Les deux callbacks ont été initialisés");
	}

	public void connected() {
		cCallback.connected(this);
	}

	public void readAutomaton() throws ClosedChannelException, IOException {
		int res = -1;

		// Penser à boucler un peu au cas où le buffer NIO contienne plusieurs
		// messages
		while (true) {

			if (currentReadingState == READING_STATE.READING_DONE)// reinit
																	// buffers
			{
				buffer_read = null;
				buffer_length.position(0); // on se repositionne au début pour
											// ecraser et réécrire dessus
				inBufferMetaData.position(0);
				currentReadingState = READING_STATE.READING_METADATA;
			}
			if (currentReadingState == READING_STATE.READING_METADATA) {
				res = socketChannel.read(inBufferMetaData);
				if (res == -1)
					throw new ClosedChannelException();
				if (res == 0)
					break;

				if (inBufferMetaData.remaining() == 0) {
					inBufferMetaData.position(0);
					inMetadata = inBufferMetaData.getInt();
					currentReadingState = READING_STATE.READING_LENGTH;
				}
			}

			if (currentReadingState == READING_STATE.READING_LENGTH) { 
				// Lecture de la taille totale du message
				res = socketChannel.read(buffer_length);
				if (res == -1)
					throw new ClosedChannelException();
				if (res == 0)
					break;

				if (buffer_length.remaining() == 0) {
					buffer_length.position(0);
					int length_msg = buffer_length.getInt();
					buffer_read = ByteBuffer.allocate(length_msg);
					currentReadingState = READING_STATE.READING_MSG;
				}
			}

			if (currentReadingState == READING_STATE.READING_MSG) {
				res = socketChannel.read(buffer_read);
				if (res == -1)
					throw new ClosedChannelException();
				if (res == 0)
					break;

				if (buffer_read.remaining() == 0) {
					// lecture complète on envoie
					// callbacke

					currentReadingState = READING_STATE.READING_CHECKSUM;
				}
			}
			if (currentReadingState == READING_STATE.READING_CHECKSUM) {
				if ((inMetadata & containChecksum) != 0) {
					res = socketChannel.read(inBufferChecksum);
					if (res == -1)
						throw new ClosedChannelException();
					if (res == 0)
						break;

					if (inBufferChecksum.remaining() == 0) {
						try {
							byte[] checksum = createChecksum(buffer_read);
							if (!checkChecksum(inBufferChecksum, checksum)) {
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
				} else {
					currentReadingState = READING_STATE.READING_DONE;
				}
			}

			if(currentReadingState==READING_STATE.READING_DONE && (inMetadata & isAheartBeat)!=0)
			{
				return;
			}
			else {
				if(heartbeat!=null)
					heartbeat.pass();
			}
			if (currentReadingState == READING_STATE.READING_DONE
					&& (inMetadata & isAMessage) != 0)
				dCallback.deliver(this, buffer_read.duplicate()); 
			// duplicate car le buffer est ecrasé pour la prochaine reception, l'utilisateur peut perdre son message
			
		}
	}

	public boolean sendAutomatton()
			throws ClosedChannelException, IOException {
		if(heartbeat==null) hb=false;
		if(!hb && heartbeat!=null) heartbeat.pass();
		// retourne true si il n'y a plus ri1en a envoyer
		// Penser à continuer l'envoi si il reste de la place dans le bufferNio
		int res = 0;
		
		while (buffers_out.size() > 0 || hb) {
			if(buffers_out.size()>0) hb=false;
			else
			{
				buffers_out.add(ByteBuffer.allocate(0));
			}
			if (currentSendingState == SENDING_STATE.SENDING_DONE) {
				currentBufferOut = buffers_out.pop();
				currentBufferOut.position(0);
				outBufferLength.position(0);
				outBufferLength.putInt(currentBufferOut.capacity());
				outBufferLength.position(0);

				outBufferMetaData.position(0);
				int metadata = hb ? isAheartBeat : outMetaData;
				outBufferMetaData.putInt(metadata);
				outBufferMetaData.position(0);

				outBufferChecksum.position(0);

				currentSendingState = SENDING_STATE.SENDING_METADATA;
			}

			if (currentSendingState == SENDING_STATE.SENDING_METADATA) {
				res = socketChannel.write(outBufferMetaData);
				if (res == 0)
					break;
				if (outBufferMetaData.remaining() == 0) {
					currentSendingState = SENDING_STATE.SENDING_LENGTH;
				}
			}

			if (currentSendingState == SENDING_STATE.SENDING_LENGTH) { // Lecture de la taille totale du message

				res = socketChannel.write(outBufferLength);
				if (res == 0)
					break;

				if (outBufferLength.remaining() == 0) {
					currentSendingState = SENDING_STATE.SENDING_MSG;
				}
			}

			if (currentSendingState == SENDING_STATE.SENDING_MSG) {

				res = socketChannel.write(currentBufferOut);
				if (res == 0)
					break;

				if (currentBufferOut.remaining() == 0) {
					currentSendingState = SENDING_STATE.SENDING_CHECKSUM;
				}
			}

			if (currentSendingState == SENDING_STATE.SENDING_CHECKSUM) {
				if ((outMetaData & containChecksum) != 0) {
					try {
						byte[] checksum = createChecksum(currentBufferOut);
						outBufferChecksum.put(checksum);
						outBufferChecksum.position(0);
						res = socketChannel.write(outBufferChecksum);
						if (res == 0)
							break;

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

		}

		return buffers_out.size() == 0
				&& currentSendingState == SENDING_STATE.SENDING_DONE;
	}

	private static byte[] createChecksum(ByteBuffer buffer)
			throws NoSuchAlgorithmException {
		MessageDigest complete = MessageDigest.getInstance("MD5");
		buffer.position(0);
		complete.update(buffer);
		buffer.position(0);
		byte[] t = complete.digest();
		return t;
	}

	private static boolean checkChecksum(ByteBuffer buffer, byte[] bytes) {
		buffer.position(0);
		byte[] bytes2 = buffer.array();

		return java.util.Arrays.equals(bytes, bytes2);
	}

}
