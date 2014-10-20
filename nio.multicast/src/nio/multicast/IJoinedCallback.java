package nio.multicast;


public interface IJoinedCallback {

	  /**
	   * Callback to notify that a previously asked group as been joined
	   */ 
	
	public void joined(IMulticastServer server,IMulticastGroup group);
}
