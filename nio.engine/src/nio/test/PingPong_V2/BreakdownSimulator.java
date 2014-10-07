package nio.test.PingPong_V2;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class BreakdownSimulator implements Runnable {

	
	private List<Thread> threads;
	private Random r;
	
	
	public BreakdownSimulator() {
		threads = new LinkedList<Thread>();
		r = new Random();
	}
	
	public void add(Thread t)
	{
		threads.add(t);
	}

	@Override
	public void run() {
		while(!Thread.interrupted())
		{
			int threadIndex = r.nextInt(threads.size());
			int timeToSleep = r.nextInt(8)*100+500;
			try {
				Thread.sleep(timeToSleep);
				//threads.get(threadIndex).interrupt();
				threads.get(threadIndex).interrupt();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
}
