package nio.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Hashtable;
import java.util.Iterator;

public class CNioEngine extends NioEngine {
	
	
	public Selector selector;
	//Hashtable<Integer,ServerSocketChannel> listening;
	Hashtable<Integer,ServerSocketChannel> listening;
	
	public CNioEngine() throws Exception {

		selector = Selector.open();
		listening = new Hashtable<Integer,ServerSocketChannel>();
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
		listening.put(port,ServerSocketChannel.open());
		InetSocketAddress isa = new InetSocketAddress("localhost", port);
		listening.get(port).socket().bind(isa);
		listening.get(port).register(selector, SelectionKey.OP_ACCEPT);
		return null;
	}

	@Override
	public void connect(InetAddress hostAddress, int port,
			ConnectCallback callback) throws UnknownHostException,
			SecurityException, IOException {
		// TODO Auto-generated method stub

	}
	
	
	/**
	 * Accept a connection and make it non-blocking
	 * @param the key of the channel on which a connection is requested
	 */
	private void handleAccept(SelectionKey key) {}
	
	/**
	 * Finish to establish a connection
	 * @param the key of the channel on which a connection is requested
	 */
	private void handleConnection(SelectionKey key) {}
	
	
	/**
	 * Handle incoming data event
	 * @param the key of the channel on which the incoming data waits to be received 
	 */
	private void handleRead(SelectionKey key) throws IOException{	}

	
	/**
	 * Handle outgoing data event
	 * @param the key of the channel on which data can be sent 
	 */
	private void handleWrite(SelectionKey key) {}
	
	
}
