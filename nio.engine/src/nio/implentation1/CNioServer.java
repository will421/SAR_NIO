package nio.implentation1;

import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.nio.channels.ServerSocketChannel;

import nio.engine.AcceptCallback;
import nio.engine.NioServer;

public class CNioServer extends NioServer {

	private ServerSocketChannel ssc;
	private AcceptCallback callback;
	
	public CNioServer(ServerSocketChannel ssc) {
		this.ssc = ssc;
	}

	public CNioServer(ServerSocketChannel ssc, AcceptCallback callback) {
		this.ssc = ssc;
		this.callback = callback;
	}

	public AcceptCallback getCallback()
	{
		return callback;
	}
	
	@Override
	public int getPort() {
		return ssc.socket().getLocalPort();
	}

	@Override
	public void close() {
		try {
			ssc.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

}
