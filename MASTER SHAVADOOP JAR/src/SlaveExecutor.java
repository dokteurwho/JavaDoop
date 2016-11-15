import java.lang.Thread.State;

public class SlaveExecutor {
	
	private Thread thread;
	private String address;
	private Job job;

	public SlaveExecutor(String address) {
		// TODO Auto-generated constructor stub
		this.address = address;
		this.thread = null;
		
	}

	public void startJob(Job job) {
		this.job = job;
		this.thread = new RunThread(this.address, job) ;
		this.thread.start();
	}
	
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
