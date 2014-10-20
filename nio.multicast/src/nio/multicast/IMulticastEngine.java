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
	
}
