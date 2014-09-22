package nio.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

public class CNioEngine extends NioEngine {
	
	
	static int BUFFER_SIZE=10000;
	
	private class Paire<T,U> {
		Paire(T premier,U second)
		{
			this.premier = premier;
			this.second = second;
		}
		public T premier;
		public U second;
		}
	
	
	
	public Selector selector;
	//Hashtable<Integer,ServerSocketChannel> listening;
	Hashtable<ServerSocketChannel,AcceptCallback> listening;
	Hashtable<SocketChannel,ConnectCallback> connecting;
	Hashtable<ServerSocketChannel,CNioServer> nioServers;
	Hashtable<SocketChannel,CNioChannel> nioChannels;
	Hashtable<SocketChannel,LinkedList<ByteBuffer>> outBuffers;
	
	public CNioEngine() throws Exception {

		selector = Selector.open();
		listening = new Hashtable<ServerSocketChannel, AcceptCallback>();
		connecting = new Hashtable<SocketChannel, ConnectCallback>();
		nioServers = new Hashtable<ServerSocketChannel, CNioServer>();
		nioChannels = new Hashtable<SocketChannel, CNioChannel>();
		// TODO Auto-generated constructor stub 
	}

	@Override
	public void mainloop() {
		while (true) {
			try {
				
				selector.select();
				Iterator<?> selectedKeys = this.selector.selectedKeys().iterator();

				while (selectedKeys.hasNext()) {

					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;

					} else if (key.isAcceptable()) {
						handleAccept(key);

					} else if (key.isReadable()) {
						handleRead(key);

					} else if (key.isWritable()) {
						handleWrite(key);

					} else if (key.isConnectable()) {
						handleConnection(key);
					} else 
						System.out.println("  ---> unknow key="+key);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public NioServer listen(int port, AcceptCallback callback)
			throws IOException {
		// TODO Ajouter la gestion de NioServer et callback
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		InetSocketAddress isa = new InetSocketAddress("localhost", port);
		ssc.socket().bind(isa);
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		CNioServer nServer = new CNioServer(ssc);
		listening.put(ssc, callback);
		nioServers.put(ssc, nServer);
		return nServer;
	}

	@Override
	public void connect(InetAddress hostAddress, int port,
			ConnectCallback callback) throws UnknownHostException,
			SecurityException, IOException {
		// TODO Auto-generated method stub
		SocketChannel sc = SocketChannel.open();
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_CONNECT);
		sc.connect(new InetSocketAddress(hostAddress, port));
		connecting.put(sc, callback);
		
	}
	
	
	/**
	 * Accept a connection and make it non-blocking
	 * @param the key of the channel on which a connection is requested
	 */
	private void handleAccept(SelectionKey key) {
		SocketChannel socketChannel = null;
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		try {
			socketChannel = serverSocketChannel.accept();
			socketChannel.configureBlocking(false);
			socketChannel.register(this.selector, SelectionKey.OP_READ);
			
			AcceptCallback callback = listening.get(serverSocketChannel);
			CNioChannel nChannel = new CNioChannel(socketChannel,this);
			nioChannels.put(socketChannel, nChannel);
			outBuffers.put(socketChannel, new LinkedList<ByteBuffer>());
			
			callback.accepted(nioServers.get(serverSocketChannel), nChannel);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} 
	}
	
	/**
	 * Finish to establish a connection
	 * @param the key of the channel on which a connection is requested
	 */
	private void handleConnection(SelectionKey key) {
		
		SocketChannel socketChannel = (SocketChannel) key.channel();

		try {
			socketChannel.finishConnect();
		} catch (IOException e) {
			// cancel the channel's registration with our selector
			System.out.println(e);
			key.cancel();
			return;
		}
		key.interestOps(key.interestOps() | SelectionKey.OP_READ);
		CNioChannel nChannel = new CNioChannel(socketChannel,this);
		nioChannels.put(socketChannel, nChannel);
		connecting.get(socketChannel).connected(nChannel);
		outBuffers.put(socketChannel, new LinkedList<ByteBuffer>());
	}
	
	
	/**
	 * Handle incoming data event
	 * @param the key of the channel on which the incoming data waits to be received 
	 */
	private void handleRead(SelectionKey key){
		
		SocketChannel socketChannel = (SocketChannel) key.channel(); 
		int length = BUFFER_SIZE;
		ByteBuffer inBuffer = ByteBuffer.allocate(length);
		int numRead;
		try {
			numRead = socketChannel.read(inBuffer);
		} catch (IOException e) { 
			// The remote forcibly closed the connection, cancel the selection key and close the channel. 
			e.printStackTrace();
			key.cancel(); 
			try {
				socketChannel.close();
			} catch (IOException e1) {
				e1.printStackTrace();
				System.exit(1);
			} 
			return; 
		} 
		
		if (numRead == -1) { 
			// Remote entity shut the socket down cleanly. Do the same from our end and cancel the channel. 
			try {
				key.channel().close();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			key.cancel(); 
			//Callback close
			return; 
		} 
		
		//Process data
		nioChannels.get(socketChannel).received(inBuffer.array(), numRead);
		
		
		/*SocketChannel socketChannel = (SocketChannel) key.channel(); 
		int length = BUFFER_SIZE;
		inBuffer = ByteBuffer.allocate(length); 

		// Attempt to read off the channel 
		int numRead; 
		try { 
			// Read up to length bytes 
			numRead = socketChannel.read(inBuffer); 
		} catch (IOException e) { 
			// The remote forcibly closed the connection, cancel the selection key and close the channel. 
			key.cancel(); 
			socketChannel.close(); 
			return; 
		} 

		if (numRead == -1) { 
			// Remote entity shut the socket down cleanly. Do the same from our end and cancel the channel. 
			key.channel().close(); 
			key.cancel(); 
			return; 
		} 

		// Process the received data, be aware that it may be incomplete 
		this.processData(this, socketChannel, this.inBuffer.array(), numRead);*/
		
	}

	
	/**
	 * Handle outgoing data event
	 * @param the key of the channel on which data can be sent 
	 */
	private void handleWrite(SelectionKey key) {
		
		
		
		/*System.out.println("handleWriteClient");
		SocketChannel socketChannel = (SocketChannel) key.channel(); 
		// outBuffer contains the data to write 
		try { 
			// Be aware thatthe write may be incomplete 
			socketChannel.write(outBuffer); 
			key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) { 
			// The channel has been closed 
			try {
				key.cancel(); 
				socketChannel.close();
			} catch (IOException e1) {
				System.out.println("Erreur à la fermeture du socket");
			} 
			return; 
		}*/
	}
	
	public void wantToWrite(CNioChannel nChannel,ByteBuffer buffer)
	{
		outBuffers.get(nChannel.getChannel()).add(buffer.duplicate());
		
		try {
			nChannel.getChannel().register(selector, SelectionKey.OP_WRITE);
		} catch (ClosedChannelException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
	}
	
	
	
}
