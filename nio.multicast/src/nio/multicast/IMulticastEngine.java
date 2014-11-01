package nio.multicast;

public interface IMulticastEngine {
	
	
	/**
	 * Ask for join a group identified by adr and port of a multicastServer
	 * @param adr
	 * @param port
	 * @param callback
	 * @return
	 */
	public IMulticastServer join(String adr, int port,IJoinedCallback callback);
	
	
	  /**
	   * NIO engine mainloop Wait for selected events on registered channels
	   * Selected events for a given channel may be ACCEPT, CONNECT, READ, WRITE
	   * Selected events for a given channel may change over time
	   */

	  public abstract void mainloop();

	
}
