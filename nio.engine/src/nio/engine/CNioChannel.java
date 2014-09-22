package nio.engine;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;



public class CNioChannel extends NioChannel /*implements AcceptCallback*/ {

	
	private ByteBuffer buffer_channel;
	private DeliverCallback callback;
	private SocketChannel socketChannel;
	private CNioEngine nEngine;

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

		buffer_channel = buf.duplicate();
	//	nEngine.wantToWrite(this, buffer_channel);
	}

	@Override
	public void send(byte[] bytes, int offset, int length) {

		while (length != 0 ){
			
			
			this.buffer_channel.put(bytes[length]);
			
			offset++;
			length--;
		}
		//nEngine.wantToWrite(this, buffer_channel);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}
	
	public void received(byte[] bytes, int length)
	{
		//Ici, l'automate
		
	}
	
/*
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
*/
}
