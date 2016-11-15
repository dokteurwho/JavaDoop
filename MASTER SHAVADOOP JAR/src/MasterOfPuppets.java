import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.Thread.State;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MasterOfPuppets {
	
	private static final String ARGS_ERROR = "Usage: exe -i input_file.txt -w working_directory -l list_of_slaves_file.csv";	
	private static final String UM_FILE = "UM_";
	private static final String SM_FILE = "SM_";
	private static final String RM_FILE = "RM_";
	
	private static List<SlaveExecutor> slaveList = new ArrayList<SlaveExecutor>();
	private static List<String> splitList = new ArrayList<String>();
	private static List<Job> mapJobList = new ArrayList<Job>();
	private static List<Job> reduceJobList = new ArrayList<Job>();
	private static String workingDir = "/Users/rom/Documents/SHAVADOOP";
	private static String slaveListFile = "/Users/rom/Documents/SHAVADOOP/List.txt";
	private static String inputFile = "/Users/rom/Documents/SHAVADOOP/ALOMBRE.txt";
	private static List<TreeMap> umxList = new ArrayList<TreeMap>();
	
	private static ShavaLog logger;
	
	private static int UMFileNb;
	
	private static int numberOfReducer;

	public MasterOfPuppets() {
		// TODO Auto-generated constructor stub
		//slaveList = new ArrayList<String>();
	}
	
	/**
	 * 
	 * @param fileName
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static TreeMap<String, Integer> readUnsortedMap(String fileName) throws ClassNotFoundException, IOException {
		File file = new File(fileName);
	    FileInputStream f = new FileInputStream(file);
	    ObjectInputStream s = new ObjectInputStream(f);
	    TreeMap<String, Integer> UMx = (TreeMap<String, Integer>) s.readObject();
	    s.close();
	    return UMx;
	}
	
	private static void mergeSMx() throws NumberFormatException, IOException 
	{		
		// Create a dictionary word -> count
		Map<String, Integer> dictionary = new TreeMap<String, Integer>();
		
		// Go through each RM file.
		for(int k=0 ; k < numberOfReducer ; k++){
			// Open RM file.
			String fileName = workingDir + RM_FILE + (k+1) + ".txt";
			File file = new File(fileName);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			// Read RM file line per line
			String line;
			while ((line = bufferedReader.readLine()) != null) {

				// line will look like alors 253
				String[] parts = line.split(" ");
				String key = parts[0]; // alors
				Integer val = (Integer) Integer.parseInt(parts[1]); // 253

				// alors 125 can be already in the list
				if(dictionary.containsKey(key)) {
					val = dictionary.get(key) + val;
				}

				dictionary.put(key, val);	
			}		
			fileReader.close();
		}

		// Create output file (input.txt.count.txt)
		PrintWriter writer = new PrintWriter(inputFile + ".count.txt");
		for (String key : dictionary.keySet()) {
			writer.println(key + " " + dictionary.get(key));
		}
		writer.close();
	}
	
	private static void UMxtoSMx() throws ClassNotFoundException, IOException {
		
		PrintWriter[] SMList = new PrintWriter[numberOfReducer];
		
		logger.Log("Creating " + numberOfReducer + " SM files.");
		
		// This will create a list of files, designed to store keys to reduce.
		for(int k=0 ; k < numberOfReducer ; k++){
			// Create SM files that will contain shuffled key.
			String fileName = workingDir + SM_FILE + (k+1) + ".txt";
			SMList[k] = new PrintWriter(fileName);

			// Create jobs that will manage the Reduce process. 1 job per file.
            Job job = new Job("Slave.jar -r " + fileName
        			+ " -o " + workingDir + RM_FILE + (k+1));
            reduceJobList.add(job);
		}
		

		logger.Log("Reading " + UMFileNb + " UM files and shuffling keys.");
		// Now we parse UM file
		for(int i = 1; i <= UMFileNb ; i++) {
			
			// Each UM is read, and keys are extracted in UMx
			TreeMap<String, Integer> UMx = readUnsortedMap(workingDir + UM_FILE + i + ".bin");
		    
			// Now for every word, we make a dispatch beyond UM files.
			// The dispatch is made on key.hashCode() % numberOfReducer.
			for(String key : UMx.keySet())
		    {
				SMList[Math.abs(key.hashCode() % numberOfReducer)].println(key + " " + UMx.get(key));
		    }			
		}
		
		// This will create a list of files, designed to store keys to reduce.
		for(int k=0 ; k < numberOfReducer ; k++){
			// Create SM files that will contain shuffled key.
			SMList[k].close();
		}		
	}
	
	/**
	 * 
	 * @param fileName		Main input file that will be split. A txt file. Cut can be done in a middle of a word.
	 * @param numSplits		Number of part to generate, the name will be SPLIT_(num).txt stored in working directory.
	 * @throws IOException
	 */
	public static void splitInputFile(String fileName, int numSplits) throws IOException {

    	RandomAccessFile raf = new RandomAccessFile(fileName, "r");
        long sourceSize = raf.length();
        long bytesPerSplit = sourceSize / numSplits ;
        long remainingBytes = sourceSize % numSplits;

        int maxReadBufferSize = 8 * 1024; //8KB
        int destIx;
        
        for(destIx=1; destIx <= numSplits; destIx++) {
        	String sxFileName = workingDir + "SPLIT_" + destIx + ".txt";
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(sxFileName));
            if(bytesPerSplit > maxReadBufferSize) {
                long numReads = bytesPerSplit/maxReadBufferSize;
                long numRemainingRead = bytesPerSplit % maxReadBufferSize;
                for(int i=0; i<numReads; i++) {
                    readWrite(raf, bw, maxReadBufferSize);
                }
                if(numRemainingRead > 0) {
                    readWrite(raf, bw, numRemainingRead);
                }
            }else {
                readWrite(raf, bw, bytesPerSplit);
            }
            bw.close();
            splitList.add(sxFileName);
            Job job = new Job("Slave.jar -m " + sxFileName
        			+ " -o " + workingDir + UM_FILE + destIx);
            mapJobList.add(job);
        }
        UMFileNb =  destIx - 1;
        
        
        if(remainingBytes > 0) {
        	String sxFileName = workingDir + "SPLIT_" + (numSplits+1) + ".txt";
            BufferedOutputStream bw = new BufferedOutputStream(new FileOutputStream(sxFileName));
            readWrite(raf, bw, remainingBytes);
            bw.close();
            
            splitList.add(sxFileName);
            Job job = new Job("Slave.jar -m " + sxFileName
        			+ " -o " + workingDir + UM_FILE + (numSplits+1));
            mapJobList.add(job);
            UMFileNb = numSplits + 1;
        }
        
        raf.close();
    }

    static void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
        byte[] buf = new byte[(int) numBytes];
        int val = raf.read(buf);
        if(val != -1) {
            bw.write(buf);
        }
    }

    
    public static void readCSVFile(String csvFile) {

        String line = "";
        String cvsSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
                // use comma as separator
                String[] content = line.split(cvsSplitBy);
         
                // Get 
                SlaveExecutor slave = new SlaveExecutor(content[0]);
                slaveList.add(slave);
                
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
		

		logger = new ShavaLog("Master");
		
		numberOfReducer = 5;
		
		// Parse command line. We expect: slave.jar -m filename -o output file name
		parseArgs(args);
		
		splitInputFile(inputFile, 4);
				
		readCSVFile(slaveListFile);
		
		
		// Run MAP
		int launchStatus = 1;
	
		do{
			launchStatus = launchJobs(mapJobList);
			Thread.sleep(100);
		} while(launchStatus == 1);
		

		logger.Log("Split done.");
		
		
		// SHUFFLE
		UMxtoSMx();
		
		do{
			launchStatus = launchJobs(reduceJobList);
			Thread.sleep(100);
		} while(launchStatus == 1);
		
		
		mergeSMx();
		
		logger.Log("Done.");
	}

	/**
	 * @throws InterruptedException 
	 * 
	 */
	private static int launchJobs(List<Job> jobList) throws InterruptedException {
		// Going through each slave
		for(SlaveExecutor slave : slaveList) {
			// A TERMINATED salve can take a new job.
			if(slave.getJobState() == State.TERMINATED) {
				// Go through jobList to find the job ready to start.
				int ret = assignJob(slave, jobList);
				if(ret == -1) {
					// No more job to execute.
					return 0;
				}
			}
		}
		return 1;
	}
	
	/**
	 * Parse the list of job, and assign a not-started job to slave
	 * @param slave
	 * @return 0 if a job has been assigned to the slave, -1 if no more job is pending.
	 * @throws InterruptedException 
	 * 
	 */
	private static int assignJob(SlaveExecutor slave, List<Job> jobList) throws InterruptedException {
		
		int ret = -1;

		for(Job job : jobList) {
			Thread.sleep(10);
			
			// A 0 status means the job is terminated correctly. Otherwise we have to run it.
			if(job.getStatus() == JobStatus.NOT_STARTED) {
				logger.Log("Starting " + job.getJobDescription());
				slave.startJob(job);
				return 0;
			}
			
			if(job.getStatus() == JobStatus.STARTED) {
				// At least 1 remaining Job, need to sure it will finish properly.
				ret = 0;
			}
		}
		
		return ret;
	}
	
	/**
	 * 
	 * @param args
	 */
	private static void parseArgs(String[] args) {
		if(args.length == 6) {
			for (int i = 0; i < 6; i += 2) {
		    	switch(args[i]) {
		    	case "-w":
		    		workingDir = args[i+1]+"/";
		    		break;
		    	case "-l":
		    		slaveListFile = args[i+1];
		    		break;
		    	case "-i":
		    		inputFile = args[i+1]; 
		    		break;
	    		default:
	    			throw new IllegalArgumentException(ARGS_ERROR);
		    	}	    	
		    }
		}
	    else
	    {
			throw new IllegalArgumentException(ARGS_ERROR);
	    }
	}

}
