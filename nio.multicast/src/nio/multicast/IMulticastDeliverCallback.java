package nio.multicast;

import java.nio.ByteBuffer;

import nio.engine.NioChannel;

public interface IMulticastDeliverCallback {

	
	
	  /**
	   * Callback to notify that a message has been received.
	   * The message is whole, all bytes have been accumulated.
	   * @param group
	   * @param bytes
	   */
	  public void deliver(IMulticastGroup group, ByteBuffer bytes);
	
	
}
