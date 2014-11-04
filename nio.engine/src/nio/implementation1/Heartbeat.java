package nio.implementation1;

import java.util.Timer;
import java.util.TimerTask;

public class Heartbeat implements Runnable {

	
	Runnable task;
	volatile TimerTask timerT;
	Timer t;
	long delay;
	volatile boolean pass;
	
	public Heartbeat(Runnable run,long delay) {
		task = run;
		t = new Timer();
		this.delay = delay;
		pass = false;
	}
	
	public void stop()
	{
		if(timerT!=null)
			timerT.cancel();
	}
	
	
	public void pass()
	{
		pass = true;
	}
	
	/*private TimerTask newTask()
	{
		TimerTask tt = new TimerTask() {
			@Override
			public void run() {
				if(!pass)
					task.run();
			}
		};
		return tt;
	}
	*/
	
	@Override
	public void run() {
		timerT = new TimerTask() {
			@Override
			public void run() {
				if(!pass)
				{
					task.run();
					System.out.println("HEARTBEAT"+this);
				}
				else 
					pass = false;
			}
		};
		t.schedule(timerT, delay, delay);
	}
}


