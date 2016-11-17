import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class RunThread extends Thread {
	
	private String exeFile ;
	private ShavaLog logger;
	private Job job;
	private SlaveExecutor currentSlave;

	/***
	 * 
	 * Initialize a ProcessBuilder on a host with a specific Job.
	 * 
	 * @param slaveExecutor.getAddress()	A host is defined by a remote computer and a path where a Slave.jar exe is stored.
	 * @param job	A job is a command that will be executed by Slave.jar
	 */
	public RunThread(SlaveExecutor slaveExecutor, Job job) {

		job.setStatus(JobStatus.STARTED);
		this.currentSlave = slaveExecutor;
		this.exeFile = job.getJobDescription();
		this.job = job;
		logger = new ShavaLog("Thread_" + System.identityHashCode(this));
	}
	
	/**
	 *  Start the ProcessBuilder
	 */
    public void run() {
		logger.Log("Creating slave [" + exeFile + "] on [" + currentSlave.getAddress() + "]");
        launchProcess();
    }

	/*
	 * Execute Slave.jar with parameters defined in Job on Slave machine.
	 */
	private  void launchProcess() {
		try {
			/*
			 * Input file can contain two type of lines:
			 *     /cal/homes/ruser/git/SANDBOX/SLAVE1 will launch a local Slave.jar in this directory.
			 *     ruser@c129-21,/cal/homes/ruser/git/SANDBOX/SLAVE2 will launch remotely Slave.jar on ruser@c129-21
			 */
			
			String[] sshInCommand = currentSlave.getAddress().split(",");
			// Either ruser@c129-21 + /cal/homes/ruser/git/SANDBOX/SLAVE2
			// or just /cal/homes/ruser/git/SANDBOX/SLAVE2
			ProcessBuilder pb;

			if(sshInCommand.length == 2) 
			{
				// ssh ruser@c129-21
				String[] command = { "/bin/bash", "-c", "ssh " + sshInCommand[0] + " java -Dfile.encoding=UTF-8 -jar " + sshInCommand[1] + "/" + exeFile};
				pb = new ProcessBuilder(command);
			}
			else
			{
				// local
				String[] command = { "/bin/bash", "-c"," java -Dfile.encoding=UTF-8 -jar " + sshInCommand[0] + "/" + exeFile};
				pb = new ProcessBuilder(command);
			}

			Process process = pb.start();
			int statusCode = process.waitFor();
			if(statusCode == 0) {
				job.setStatus(JobStatus.FINISHED);
				currentSlave.slaveSuccess();
				logger.Log("Process finished with status: Success" +
						" and returned: " + output(process.getInputStream()));
			}
			else {
				// The pause prevent from looping on a non responding slave.
				Thread.sleep(500);
				// Increase number of failures
				currentSlave.salveFailed();
				// Resume job status.
				job.setStatus(JobStatus.NOT_STARTED);	
				logger.Log("Process finished with status: Failed" +
						" and returned: " + output(process.getErrorStream()));				
			}
		
		} catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
	}
	
	private  String output(InputStream inputStream) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line + System.getProperty("line.separator"));
			}
		} finally {
			br.close();
		}
		return sb.toString();
	}

}
