package nio.implentation1;

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

import nio.engine.AcceptCallback;
import nio.engine.ConnectCallback;
import nio.engine.NioEngine;
import nio.engine.NioServer;

public class CNioEngine extends NioEngine {

	static int BUFFER_SIZE = 10000;

	public Selector selector;
	//Hashtable<ServerSocketChannel, AcceptCallback> listening;
	Hashtable<SocketChannel, ConnectCallback> connecting;
	Hashtable<ServerSocketChannel, CNioServer> nioServers;
	Hashtable<SocketChannel, CNioChannel> nioChannels;

	public CNioEngine() throws Exception {


		selector = Selector.open();
		//listening = new Hashtable<ServerSocketChannel, AcceptCallback>();
		connecting = new Hashtable<SocketChannel, ConnectCallback>();
		nioServers = new Hashtable<ServerSocketChannel, CNioServer>();
		nioChannels = new Hashtable<SocketChannel, CNioChannel>();

	}

	@Override
	public void mainloop() {
		while (true) {
			try {

				selector.select();
				Iterator<?> selectedKeys = this.selector.selectedKeys()
						.iterator();

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
						System.out.println("  ---> unknow key=" + key);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}

	}

	@Override
	public NioServer listen(int port, AcceptCallback callback)
			/*throws IOException*/ {
		CNioServer nServer = null;
		try {
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			InetSocketAddress isa = new InetSocketAddress("localhost", port);
			ssc.socket().bind(isa);
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			nServer = new CNioServer(ssc,callback);
			//listening.put(ssc, callback);
			nioServers.put(ssc, nServer);
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return nServer;
	}

	@Override
	public void connect(InetAddress hostAddress, int port,
			ConnectCallback callback)/* throws UnknownHostException,
			SecurityException, IOException*/ {
		// TODO Auto-generated method stub

		SocketChannel sc;
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(false);
			sc.register(selector, SelectionKey.OP_CONNECT);
			sc.connect(new InetSocketAddress(hostAddress, port));
			connecting.put(sc, callback);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Accept a connection and make it non-blocking
	 * 
	 * @param the
	 *            key of the channel on which a connection is requested
	 */
	private void handleAccept(SelectionKey key) {
		SocketChannel socketChannel = null;
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key
				.channel();
		try {
			socketChannel = serverSocketChannel.accept();
			socketChannel.configureBlocking(false);
			socketChannel.register(this.selector, SelectionKey.OP_READ);

			AcceptCallback callback = nioServers.get(serverSocketChannel).getCallback();
			CNioChannel nChannel = new CNioChannel(socketChannel, this);
			nioChannels.put(socketChannel, nChannel);
			

			callback.accepted(nioServers.get(serverSocketChannel), nChannel);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Finish to establish a connection
	 * 
	 * @param the
	 *            key of the channel on which a connection is requested
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
		key.interestOps(SelectionKey.OP_READ);
		int a =SelectionKey.OP_CONNECT;
		int b= SelectionKey.OP_ACCEPT;
		int c = SelectionKey.OP_READ;
		int d = SelectionKey.OP_WRITE;
		
		CNioChannel nChannel  = new CNioChannel(socketChannel, this);
		nioChannels.put(socketChannel, nChannel);
		connecting.get(socketChannel).connected(nChannel);
		
	}

	/**
	 * Handle incoming data event
	 * 
	 * @param the
	 *            key of the channel on which the incoming data waits to be
	 *            received
	 * @throws IOException
	 */
	private void handleRead(SelectionKey key) throws IOException {

		SocketChannel socketChannel = (SocketChannel) key.channel();

		nioChannels.get(socketChannel).readAutomaton();

	}

	/**
	 * Handle outgoing data event
	 * 
	 * @param the
	 *            key of the channel on which data can be sent
	 */
	private void handleWrite(SelectionKey key) throws IOException {


		SocketChannel socketChannel = (SocketChannel) key.channel();
		CNioChannel nChannel = nioChannels.get(socketChannel);
		boolean res = nChannel.sendAutomatton();
		
		if(res) 
			// Pour retirer l'interet en ecriture, uniquement dans le cas où plus rien à ecrire
			// Dependera de la valeur de retour de automate
			key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
	}

	
	public void wantToWrite(CNioChannel nChannel)
	{
		try {
				nChannel.getChannel().register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
		} catch (ClosedChannelException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
