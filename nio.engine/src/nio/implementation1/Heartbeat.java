package nio.implementation1;

import java.util.Timer;
import java.util.TimerTask;

public class Heartbeat implements Runnable {

	
	Runnable task;
	TimerTask timerT;
	Timer t;
	long delay;
	
	public Heartbeat(Runnable run,long delay) {
		task = run;
		t = new Timer();
		this.delay = delay;
	}
	
	synchronized public void reset()
	{
		if(timerT!=null)
			timerT.cancel();
		timerT = newTask();
		t.schedule(timerT, delay, delay);
		//t.sc
	}
	
	private TimerTask newTask()
	{
		TimerTask tt = new TimerTask() {
			@Override
			public void run() {
				task.run();
			}
		};
		return tt;
	}
	
	
	@Override
	public void run() {
		reset();
	}
}


