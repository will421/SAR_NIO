package nio.multicast;

import java.nio.ByteBuffer;



public interface IMulticastGroup {

	
	
	  /**
	   * Set the callback to deliver messages to.
	   * @param callback
	   */
	  public abstract void setDeliverCallback(IMulticastDeliverCallback callback);
	
	
	  /**
	   * Send the given byte buffer. No copy is made, so the buffer 
	   * should no longer be used by the code sending it.
	   * @param buf
	   */
	  public abstract void send(ByteBuffer buf);

	  /**
	   * Sending the given byte array, a copy is made into internal buffers,
	   * so the array can be reused after sending it.
	   * @param bytes
	   * @param offset
	   * @param length
	   */
	  public abstract void send(byte[] bytes, int offset, int length);
	  
	  /**
	   * Ask for leaving the group and do not receive message from it
	   */
	  public void leave();
	
	
}
