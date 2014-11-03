package nio.multicast.implementation;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map.Entry;

import nio.engine.NioChannel;

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
	private boolean[] acks;
	

	public MulticastQueueElement(ByteBuffer bytes,int groupSize) {
		bytes.position(0);
		type = MESSAGE_TYPE.values()[bytes.getInt()];
		clock = bytes.getLong();
		pid = bytes.getInt();
		ByteBuffer tmp = bytes.slice();
		message = ByteBuffer.allocate(tmp.remaining());
		message.put(tmp);
		acks = new boolean[groupSize];
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
		acks = new boolean[groupSize];
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
		acks[pidS]=true;
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
	
	public ByteBuffer getMessage()
	{
		return message;
	}
	 
	public MESSAGE_TYPE getType()
	{
		return type;
	}
	
	public int getAcksMask()
	{
		int res = 0;
		for(int i =0;i<acks.length;i++)
		{
			if(acks[i])
			{
				res = res | (int)Math.pow(2, i);
			}
			else
			{
				
			}
		}
		return res;
	}
}