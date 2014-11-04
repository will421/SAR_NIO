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
	private NioChannel channelInLocal;
	private int pidLocal;

	public ListMember(int length) {
		this.length = length;
		adrs= new String[length];
		ports = new int[length];
		channels = new NioChannel[length];
		channelInLocal = null;
		valid = new boolean[length];

		for(int i=0;i<length;i++)
		{
			adrs[i] = null;
			ports[i] = -1;
			channels[i] = null;
			valid[i] = false;
		}

	}

	public void setChannelInLocal(NioChannel ch,int pid)
	{
		channelInLocal = ch;
		pidLocal = pid;
	}
	
	public NioChannel getChannelInLocal()
	{
		return channelInLocal;
	}
	
	public int getPid(NioChannel channel)
	{
		int res=-1;
		if(channel == channelInLocal)
			return pidLocal;
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

	@SuppressWarnings("unused")
	public int disconnected(NioChannel channel)
	{
		int pid = getPid(channel);
		if(pid==-1)
		{
			int a=1;
		}
		channels[pid] = null;
		valid[pid] = false;
		adrs[pid] = null;
		ports[pid] = -1;
		return pid;
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
		return b && (channelInLocal!=null);
	}

	public boolean contains(NioChannel channel)
	{
		return Arrays.asList(channels).contains(channel);
	}

	public int getMask()
	{
		int mask = 0;
		for(int i =0;i<length;i++)
		{
			if(channels[i]==null)
			{}
			else
			{
				mask = mask | (int)Math.pow(2, i);
			}
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