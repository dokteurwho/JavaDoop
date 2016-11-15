


public class Job {
	
	private JobStatus status;
	private String jobDescription;
	
	public Job(String jobDescription) {
		this.status = JobStatus.NOT_STARTED;
		this.jobDescription = jobDescription;
	}
	
	
	public String getJobDescription() {
		return jobDescription;
	}


	public JobStatus getStatus() {
		return this.status;
	}


	public void setStatus(JobStatus status) {
		this.status = status;
	}

}
