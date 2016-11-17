import java.lang.Thread.State;

public class SlaveExecutor {
	
	private Thread thread;
	private String address;
	private Job job;
	int failureNb;
	int successNb;
	int priority;

	public SlaveExecutor(String address) {
		// TODO Auto-generated constructor stub
		this.address = address;
		this.thread = null;
		failureNb = 0;
		successNb = 0;
		priority = 5;
	}
	
	public void slaveSuccess() {
		successNb ++;
	}
	
	public float getScore() {
		int t = failureNb + successNb;
		if(t == 0)
			return 1;
		else
			return successNb / (failureNb + successNb);
	}
	
	public void salveFailed() {
		failureNb ++;
	}
	
	public String getAddress()  {
		return this.address;
	}

	/**
	 * Launch a Thread with a job.
	 * @param job
	 */
	public void startJob(Job job) {
		this.job = job;
		this.thread = new RunThread(this, job) ;
		this.thread.start();
	}
	
	/**
	 * Return associated thread state.
	 * 
	 * @return TERMINATED, ...
	 */
	public State getJobState() {
		if(this.thread == null)
			return State.TERMINATED;
		else
			return this.thread.getState();
	}
	
	public Job getJob() {
		return this.job;
	}
	

}
