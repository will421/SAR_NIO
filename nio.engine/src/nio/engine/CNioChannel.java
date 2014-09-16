package nio.engine;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;



public class CNioChannel extends NioChannel implements AcceptCallback {

	
	ByteBuffer buffer_channel;
	
	@Override
	public SocketChannel getChannel() {

		return null;
	}

	@Override
	public void setDeliverCallback(DeliverCallback callback) {
		// TODO Auto-generated method stub

	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void send(ByteBuffer buf) {

		buffer_channel = buf.duplicate();

	}

	@Override
	public void send(byte[] bytes, int offset, int length) {

		while (length != 0 ){
			
			
			this.buffer_channel.put(bytes[length]);
			
			offset++;
			length--;
		}
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

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

}
