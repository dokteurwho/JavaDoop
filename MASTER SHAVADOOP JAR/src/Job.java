/**
 * Job is a very simple class allowing a thread to "take" a job, defined by a command line by a jobDescription.
 * The jobDescription can be then processed by ProcessBuilder.
 * Ironically this is not a multithreadable class.
 * 
 * @author rpicon
 *
 */


public class Job {
	
	private JobStatus status;
	private String jobDescription;
	/**
	 * Create a new job in NOT_STARTED status.
	 * 
	 * @param jobDescription	A command to launch in process builder. For example: Slave.jar -r in_file -o out_file
	 */
	public Job(String jobDescription) {
		this.status = JobStatus.NOT_STARTED;
		this.jobDescription = jobDescription;
	}
	
	/**
	 * 
	 * @return	The jobDescription, to invoke in a ProcessBuilder.
	 */
	public String getJobDescription() {
		return jobDescription;
	}

	/**
	 * A NOT_STARTED job can be assigned to a Slave.
	 * A STARTED job is currently assigned to a Slave. Next transition is FINISHED or NOT_STARTED if ProcessBuilder failed.
	 * 
	 * @return JobStatus NOT_STARTED, STARTED, FINISHED 
	 */
	public JobStatus getStatus() {
		return this.status;
	}

	/**
	 * Set by Slave. 
	 * STARTED means the job is being processed. 
	 * FINISHED means the job has been successfully achieved ie the ProcessBuilder returned success.
	 * NOT_STARTED if the ProcessBuilder returned an error.
	 * 
	 * @param status JobStatus NOT_STARTED, STARTED, FINISHED
	 */
	public void setStatus(JobStatus status) {
		this.status = status;
	}

}
