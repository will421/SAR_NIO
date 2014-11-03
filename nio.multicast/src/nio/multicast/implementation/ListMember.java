package nio.multicast.implementation;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import nio.engine.NioChannel;

public class ListMember{
	public int length;

	private boolean valid[];
	private String adrs[];
	private int ports[];
	public NioChannel channels[]; //remettre priver


	public ListMember(int length) {
		this.length = length;
		adrs= new String[length];
		ports = new int[length];
		channels = new NioChannel[length];
		valid = new boolean[length];

		for(int i=0;i<length;i++)
		{
			adrs[i] = null;
			ports[i] = -1;
			channels[i] = null;
			valid[i] = false;
		}

	}

	public int getPid(NioChannel channel)
	{
		int res=-1;
		for(int i=0;i<length;i++)
		{
			if(channels[i]==channel)
			{
				res = i;
				break;
			}
		}
		return res;
	}
	
	public boolean isConnected(int pid)
	{
		return channels[pid]!=null;
	}

	public void connected(int pid,NioChannel channel)
	{
		channels[pid] = channel;
	}

	public void disconnected(NioChannel channel)
	{
		int pid = getPid(channel);
		channels[pid] = null;
		valid[pid] = false;
		adrs[pid] = null;
		ports[pid] = -1;
	}

	public void addMember(int pid,String adr,int port)
	{
		adrs[pid] = adr;
		ports[pid] = port;
		valid[pid] = true;
	}

	public String getAdr(int pid)
	{
		return adrs[pid];
	}

	public int getPort(int pid)
	{
		return ports[pid];
	}
	public NioChannel getChannel(int pid)
	{
		return channels[pid];
	}

	public boolean full()
	{
		boolean b = true;
		for(NioChannel ch : channels)
		{
			if(ch==null)
			{
				b = false;
				break;
			}
		}
		return b;
	}

	public boolean contains(NioChannel channel)
	{
		return Arrays.asList(channels).contains(channel);
	}

	public byte[] getMask()
	{
		byte[] mask = new byte[length];
		for(int i =0;i<length;i++)
		{
			if(channels[i]==null)
				mask[i]=0;
			else
				mask[i] =1;

		}
		return mask;
	}
	
	public List<Integer> getPIDS()
	{
		List<Integer> res = new LinkedList<Integer>();
		for(int i = 0;i<length;i++)
		{
			if(channels[i]!=null)
				res.add(i);
		}
		return res;
	}

}