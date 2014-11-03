package nio.multicast;

import java.nio.ByteBuffer;
import java.util.List;

public interface IMulticastEngine {
	
	
	/**
	 * Ask for join a group identified by adr and port of a multicastEntryServer
	 * @param adr
	 * @param port
	 * @param callback
	 * @return
	 */
	public void join(String adr, int port,IMulticastCallback callback);


	/**
	 * NIO engine mainloop Wait for selected events on registered channels
	 * Selected events for a given channel may be ACCEPT, CONNECT, READ, WRITE
	 * Selected events for a given channel may change over time
	 */

	public abstract void mainloop();

	
	  /**
	   * Send the given byte buffer. No copy is made, so the buffer 
	   * should no longer be used by the code sending it.
	   * Send only if joined as occured
	   * @param buf
	   */
	  public abstract void send(ByteBuffer buf);

	  /**
	   * Sending the given byte array, a copy is made into internal buffers,
	   * so the array can be reused after sending it.
	   * Send only if joined as occured
	   * @param bytes
	   * @param offset
	   * @param length
	   */
	  public abstract void send(byte[] bytes, int offset, int length);

	  /**
	   * Ask for leaving the group and do not receive message from it
	   */
	  public void leave();


	  public List<Integer> getPIDS();
	
}
