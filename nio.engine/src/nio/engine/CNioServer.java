package nio.engine;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

public class CNioServer extends NioServer {

	private ServerSocketChannel ssc;
	
	
	public CNioServer(ServerSocketChannel ssc) {
		this.ssc = ssc;
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
