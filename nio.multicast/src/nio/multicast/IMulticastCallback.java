package nio.multicast;

import java.nio.ByteBuffer;

import nio.engine.NioChannel;

public interface IMulticastCallback {


	  /**
	   * Callback to notify that a message has been received.
	   * The message is whole, all bytes have been accumulated.
	   * @param group
	   * @param bytes
	   */
	  public void deliver(IMulticastEngine engine, ByteBuffer bytes);
	
	  /**
	   * Callback to notify that the engine is ready
	   */ 
	  public void joined(int pid);
	  
	  /*
	   * 
	   */
	  public void memberJoin(int pid);
	  
	  public void memberQuit(int pid);

	  public void disconnected();
	  
}
