package nio.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.Iterator;

public class CNioEngine extends NioEngine {
	
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
	Hashtable<Integer,Paire<NioServer,AcceptCallback>> listening;
	Hashtable<SocketChannel,ConnectCallback> connecting;
	
	public CNioEngine() throws Exception {

		selector = Selector.open();
		listening = new Hashtable<Integer, CNioEngine.Paire<NioServer,AcceptCallback>>();
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
		NioServer nServer = new CNioServer(ssc);
		Paire<NioServer,AcceptCallback> p = new Paire<NioServer,AcceptCallback>(nServer,callback);
		listening.put(port, p);
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
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		try {
			socketChannel.register(this.selector, SelectionKey.OP_READ);
		} catch (ClosedChannelException e) {
			e.printStackTrace();
			System.exit(1);
		}
		Paire<NioServer,AcceptCallback> p = listening.get(serverSocketChannel.socket().getLocalPort());
		NioChannel nChannel = new CNioChannel(socketChannel);
		p.second.accepted(p.premier, nChannel);
		
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
		NioChannel nChannel = new CNioChannel(socketChannel);
		connecting.get(socketChannel).connected(nChannel);
	}
	
	
	/**
	 * Handle incoming data event
	 * @param the key of the channel on which the incoming data waits to be received 
	 */
	private void handleRead(SelectionKey key) throws IOException{
		//Automate etc
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		//deliveredcallback
		
	}

	
	/**
	 * Handle outgoing data event
	 * @param the key of the channel on which data can be sent 
	 */
	private void handleWrite(SelectionKey key) {}
	
	
}
