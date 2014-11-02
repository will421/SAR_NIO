package nio.multicast.implementation;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map.Entry;

import nio.engine.NioChannel;
import nio.multicast.implementation.MulticastEngine.MESSAGE_TYPE;

/**
 * Represente un element de la queue de message multicast d'un engine
 * @author Will
 *
 */
public class MulticastQueueElement implements Comparable<MulticastQueueElement> {
	private MESSAGE_TYPE type;
	private ByteBuffer message;
	private long clock;
	private int pid;
	private byte[] acks;
	

	public MulticastQueueElement(ByteBuffer bytes,int groupSize) {
		bytes.position(0);
		type = MESSAGE_TYPE.values()[bytes.getInt()];
		clock = bytes.getLong();
		pid = bytes.getInt();
		message = bytes.slice();
		acks = new byte[groupSize];
	}
	
	/**
	 * Création d'un element sans message dans le cas où reception d'un ack mais en attente du message
	 */
	public MulticastQueueElement(long clock,int pid,int groupSize)
	{
		this.type = MESSAGE_TYPE.NOT_RECEIVED_YET;
		this.clock = clock;
		this.pid = pid;
		message = null;
		acks = new byte[groupSize];
	}
	
	public static MulticastQueueElement getElement(List<MulticastQueueElement> list,long clock,int pidM)
	{
		for(MulticastQueueElement el : list) {
		    if(el.clock == clock && el.pid == pidM)
		    {
		    	return el;
		    }
		}
		return null;
	}
	
	public void ackReceived(int pidS)
	{
		acks[pidS]=1;
	}
	
	public long getClock()
	{
		return clock;
	}
	@Override
	public int compareTo(MulticastQueueElement o) {
		MulticastQueueElement elm = (MulticastQueueElement)o;
		if(this.clock<elm.clock)
			return -1;
		else if(this.clock>elm.clock)
			return 1;
		else
		{
			if(this.pid<elm.pid)
				return -1;
			else if(this.pid>elm.pid)
				return 1;
			else
			{
				try {
					throw new Exception("Should not occur");
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
				return 0;
			}
		}
	}
	 
}