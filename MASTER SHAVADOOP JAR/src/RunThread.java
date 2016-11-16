import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


public class RunThread extends Thread {
	
	private String targetHost;
	private String exeFile ;
	private ShavaLog logger;
	private Job job;

	public RunThread(String host, Job job) {

		job.setStatus(JobStatus.STARTED);
		this.targetHost = host;
		this.exeFile = job.getJobDescription();
		this.job = job;
		logger = new ShavaLog("Thread_" + System.identityHashCode(this));
	}
	
    public void run() {
		logger.Log("Creating slave [" + exeFile + "] on [" + targetHost + "]");
        launchProcess();
    }


	private  void launchProcess() {
		try {
			
			//String[] command = { "/bin/bash", "-c", "ssh rpicon@" + targetHost + " java -Dfile.encoding=UTF-8 -jar " + exeFile};
			String[] command = { "/bin/bash", "-c"," java -Dfile.encoding=UTF-8 -jar " + targetHost + "/" + exeFile};
			ProcessBuilder pb = new ProcessBuilder(command);

			logger.Log("Process starts");
			Process process = pb.start();
			int statusCode = process.waitFor();
			if(statusCode == 0) {
				job.setStatus(JobStatus.FINISHED);	
				logger.Log("Process finished with status: Success" +
						" and returned: " + output(process.getInputStream()));
			}
			else {
				// The pause prevent from looping on a non responding slave.
				Thread.sleep(3000);
				// Resume job status.
				job.setStatus(JobStatus.NOT_STARTED);	
				logger.Log("Process finished with status: Failed" +
						" and returned: " + output(process.getInputStream()));				
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
