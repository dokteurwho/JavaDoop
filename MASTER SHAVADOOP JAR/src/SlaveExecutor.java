import java.lang.Thread.State;


/**
 * SlaveExecutor is defined by an address identifying where Slave.JAR is located.
 * This location can be a local folder or a folder on a distant machine.
 * 
 * "user@c130-25,/cal/homes/user/git/SANDBOX/SLAVE3" will remotely connect to c130-25 using "user" credentials.
 * "/cal/homes/user/git/SANDBOX/SLAVE1" will search Slave.JAR on the local computer.
 *
 * @author rom
 *
 */
public class SlaveExecutor {
	
	private Thread thread;
	private String address;
	private Job job;
	int failureNb;
	int successNb;
	int priority;

	/**
	 * Creates a SlaveExecutor object. A SlaveExecutor is defined by a directory address where the Slave.JAR is locate and optionally a ssh login.
	 * 
	 * "user@c130-25,/cal/homes/user/git/SANDBOX/SLAVE3" will remotely connect to c130-25 using "user" credentials.
	 * "/cal/homes/user/git/SANDBOX/SLAVE1" will search Slave.JAR on the local computer.
	 * 
	 * @param address	Directory address where Slave.JAR is located. "[user@hostname,]directory"
	 */
	public SlaveExecutor(String address) {
		// TODO Auto-generated constructor stub
		this.address = address;
		this.thread = null;
		failureNb = 0;
		successNb = 0;
		priority = 5;
	}
	
	/**
	 * Master can commit a successfully executed Job. This will increase SlaveExecutor priority in round-robin process.
	 */
	public void slaveSuccess() {
		successNb ++;
	}
	
	/**
	 * 
	 * SlaveExecutor score is returned allowing Master to elect the best SlaveExecutor.
	 * 
	 * @return	SlaveExecutor score, defined by successNb / (failureNb + successNb)
	 */
	public float getScore() {
		int t = failureNb + successNb;
		if(t == 0)
			return 1;
		else
			return successNb / (failureNb + successNb);
	}
	
	/**
	 * 
	 * Master can commit a failed Job. This will decrease SlaveExecutor priority in round-robin process.
	 */
	public void salveFailed() {
		failureNb ++;
	}
	
	/**
	 * 
	 * Master can obtain SlaveExecutor address. Example:
	 * 
	 * "user@c130-25,/cal/homes/user/git/SANDBOX/SLAVE3" will remotely connect to c130-25 using "user" credentials.
	 * "/cal/homes/user/git/SANDBOX/SLAVE1" will search Slave.JAR on the local computer.
	 * 
	 * @return 	SlaveExecutor address.
	 * 	 
	 * */
	public String getAddress()  {
		return this.address;
	}

	/**
	 * Attach and launch Thread of SlaveExecutor to a Job. The Job must be in state: JobStatus.NOT_STARTED
	 * @param job A Job object.
	 */
	public void startJob(Job job) {
		this.job = job;
		this.thread = new RunThread(this, job) ;
		this.thread.start();
	}
	
	/**
	 * Return associated thread state.
	 * 
	 * @return A Thread.State : TERMINATED, ...
	 */
	public State getJobState() {
		if(this.thread == null)
			return State.TERMINATED;
		else
		
			return this.thread.getState();
	}
	/**
	 * Return Job object associate by startJob() 
	 * 
	 * @return current Job object.
	 */
	public Job getJob() {
		return this.job;
	}
	

}
