package nio.multicast;

public interface IMulticastServer {

	/**
	 * Represent a multicastServer for another process
	 */
	
	
	  /**
	   * @return the port onto which connections are accepted.
	   */
	  public abstract int getPort();
	    
	  /**
	   * Close the server port, no longer accepting connections.
	   */
	  public abstract void close();
	
}
