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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Master process.
 * 
 * Apply the following transformations:
 * 
 * Input File -> SPLIT_X.txt
 * Call Slave.jar SPLIT_X.txt -> UM_Y.txt
 * UM_Y.txt -> SM_Z.txt
 * Call Slave.jar SM_Z.txt -> RM_S.txt
 * All RM_S.txt -> Output Fie
 * 
 * @author rom
 *
 */
public class MasterOfPuppets {

	private static final String ARGS_ERROR = "Usage: "
			+ "\r\nMaster.jar [options] "
			+ "\r\nMandatory:"
			+ "\r\n-inputfile -i <file>		input file, txt format."
			+ "\r\n-workingdir -w <directory>	working directory where all file will be generated "
			+ "\r\n-slavelist -l <file> 		slave list "
			+ "\r\nOptions:"
			+ "\r\n-stopwordsfile -s <file>	file containing stop words, example here: http://snowball.tartarus.org/algorithms/french/stop.txt "
			+ "\r\n-nbreducers -r  integer 	number of reducers, 5 by default"
			+ "\r\n-nblines -n integer  		number of line per splitted file, 1000 by default";

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

	private static String stopWordsFile = "";
	private static List<String> stopWordsList = new ArrayList<String>();
	
	private static List<TreeMap> umxList = new ArrayList<TreeMap>();
	
	private static ShavaLog logger;

	private static int UMFileNb;
	private static int numberOfReducer;
	private static int numberOfLinePerFile;

	public MasterOfPuppets() {
		// TODO Auto-generated constructor stub
		//slaveList = new ArrayList<String>();
	}

	/**
	 * Read UM_X.bin to generate a TreeMap.
	 * Where UM_X.bin has been geneated by Slave.
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
	
	/**
	 * Comparator to sort TreeMap per value.
	 * 
	 * From "TreeMap sort by value topic" in http://stackoverflow.com/
	 * 
	 * @param map
	 * @return
	 */
	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(final Map<K, V> map) {
	    Comparator<K> valueComparator =  new Comparator<K>() {
	        public int compare(K k1, K k2) {
	            int compare = map.get(k2).compareTo(map.get(k1));
	            if (compare == 0) return 1;
	            else return compare;
	        }
	    };
	    Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
	    sortedByValues.putAll(map);
	    return sortedByValues;
	}
	
	/**
	 * mergeSMx() corresponds to the last Map / Reduce step.
	 * mergeSMx() will aggregate (reduce) all RM files into a final file.
	 * 
	 * A RM file is a file in working directory named as RM_i.txt where i is a number.
	 * 
	 * The output file is inputFile + .count.txt. For example: AmericanPsycho.txt.count.txt.
	 * 
	 * If a "stop words" file has been defined, stop words will be removed at this step.
	 * 
	 * @throws NumberFormatException
	 * @throws IOException
	 */
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
				if(key.length() > 1) {
					if(key.matches(".*\\d+.*") == false) {
						if(stopWordsList.contains(key) == false) {
							if(dictionary.containsKey(key)) {
								val = dictionary.get(key) + val;
							}
							dictionary.put(key, val);	
						}
					}
				}
			}		
			fileReader.close();
		}
		
		// Create a dictionary word -> count
		Map<String, Integer> dictionarySorted = new TreeMap<String, Integer>();
		dictionarySorted = sortByValues(dictionary);

		// Create output file (input.txt.count.txt)
		logger.Log("Saving results in " + inputFile + ".count.txt");
		logger.Log("TOP#15 words:");
		int displayLimit = 0;
		PrintWriter writer = new PrintWriter(inputFile + ".count.txt");
		for (String key : dictionarySorted.keySet()) {
			String line = (key + "\t" + dictionary.get(key));
			writer.println(line);
			if(displayLimit < 50)
			{
				logger.Log("#" + displayLimit + "   " + (line+1));
				displayLimit++;
			}
		}
		writer.close();
	}

	/**
	 * UMxtoSMx will split unique key beyond files. 
	 * 
	 * These files will be stored in the working directory with the following name SM_i.txt.
	 * 
	 * Example for SM_1.txt
	 * hello 2
	 * world 1
	 * hello 3
	 * 
	 * These files will be then sent to slaves for aggregation.
	 * 
	 * The partitioning is based on the word hash and the number of file to generate defined by numberOfReducer.
	 * 
	 * For example if numberOfReducer is 10, 10 files will be created with the same number of unique words.
	 * 
	 * Slaves will generate RM_i.txt files accordingly.
	 * 
	 * Example for RM_1.txt
	 * hello 5
	 * world 1
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
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
	 * splitInputFile simply splits fileName content in numSplits distinct files.
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

	/**
	 * splitInputFile simply splits fileName content in N distinct files.
	 * Where N = number of line of fileName / numLines
	 * 
	 * @param fileName		Main input file that will be split. A txt file. .
	 * @param numLines		Number of line per file, the name will be SPLIT_(num).txt stored in working directory.
	 * @throws IOException
	 */
	public static void splitInputFile2(String fileName, int numLines) throws IOException {

		File file = new File(fileName);
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);

		int destIx = 0;

		// Read RM file line per line
		String line;
		int currentLine;
		String sxFileName;

		do {
			currentLine = 0;
			// Create output file (input.txt.count.txt)
			destIx ++;
			sxFileName = workingDir + "SPLIT_" + destIx + ".txt";
			PrintWriter writer = new PrintWriter(sxFileName);

			while ((line = bufferedReader.readLine()) != null && currentLine < numLines) 
			{
				writer.println(line);
				currentLine ++;
			}
			// Be sure we don't forget a line during the split.
			if(line != null) {
				writer.println(line);
			}
			writer.close();

			// Create a Job
			Job job = new Job("Slave.jar -m " + sxFileName
					+ " -o " + workingDir + UM_FILE + destIx);
			mapJobList.add(job);
		} while(line != null);

		// Clean


		fileReader.close();

		UMFileNb = destIx;
	}

	/**
	 * 
	 * From Stackoverflow.com
	 * 
	 * @param raf
	 * @param bw
	 * @param numBytes
	 * @throws IOException
	 */
	static void readWrite(RandomAccessFile raf, BufferedOutputStream bw, long numBytes) throws IOException {
		byte[] buf = new byte[(int) numBytes];
		int val = raf.read(buf);
		if(val != -1) {
			bw.write(buf);
		}
	}


	/**
	 * 
	 * Read a ";" separated file.
	 * 
	 * @param csvFile
	 */
	public static void readCSVFile(String csvFile) {

		String line = "";
		String cvsSplitBy = ";";

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
	 * 
	 * -w /cal/homes/rpicon/git/SANDBOX 
	 * -l /cal/homes/rpicon/git/SANDBOX/slavelist.txt 
	 * -i /cal/homes/rpicon/git/SANDBOX/LesMiserables.txt  
	 * -s /cal/homes/rpicon/git/SANDBOX/stop.txt
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ClassNotFoundException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {


		// Will be redirected to stdout
		logger = new ShavaLog("Master");

		// By default.
		numberOfReducer = 5;
		numberOfLinePerFile = 1000;

		// Parse command line. We expect: slave.jar -m filename -o output file name
		parseArgs(args);
		
		// Just a reminder.
		logger.Log("------ Configuration ------ ");
		logger.Log("Input file: " + inputFile);
		logger.Log("Working directory: " + workingDir);
		logger.Log("Slave list : " + slaveListFile);
		logger.Log("Number of reducer  (optional, default 5): " + numberOfReducer);
		logger.Log("Number of lines per files  (optional, default 1000): " + numberOfLinePerFile);
		logger.Log("Stop words list (optional, default none): " + stopWordsFile);
		

		logger.Log("------ Creating slaves list ------ ");
		readCSVFile(slaveListFile);
		
		logger.Log("------ Creating stop words list ------ ");
		readStopWordsFile(stopWordsFile);

		logger.Log("------ Splitting files ------ ");
		splitInputFile2(inputFile, numberOfLinePerFile);
		// We have many SPLIT_x.txt files


		// Run MAP: SPLIT_x.txt -> UM_x.bin
		logger.Log("------ Starting map process ------ ");
		int launchStatus = 1;
		do{
			// All job must FINISHED to release this loop.
			launchStatus = launchJobs(mapJobList);
			Thread.sleep(10);
		} while(launchStatus == 1);
		logger.Log("------ Map process done------ ");


		// SHUFFLE
		logger.Log("------ Shuffling ------ ");
		UMxtoSMx();
		logger.Log("------ Shuffling done ------ ");


		logger.Log("------ Starting remote reducing ------ ");
		do{
			launchStatus = launchJobs(reduceJobList);
			Thread.sleep(10);
		} while(launchStatus == 1);
		logger.Log("------ Remote reducing done------ ");


		logger.Log("------ Starting final merge ------ ");
		mergeSMx();
		logger.Log("------ Final merge done ------");
	}

	/**
	 * Read the content of a "stop words" file.
	 * 
	 * Character | is considered as a comment.
	 * 
	 * Example:
	 * 
	 *  | A French stop word list. Comments begin with vertical bar. Each stop
 	 *  | word is at the start of a line.
	 *  
	 *  au             |  a + le
	 *  aux            |  a + les
	 *  avec           |  with
	 *  ce             |  this
	 *  ces            |  these
	 * 
	 * @param stopWordsFile2
	 * @throws IOException 
	 */
	private static void readStopWordsFile(String stopWordsFile2) throws IOException {
		
		if(stopWordsFile2.length() > 0) {
			File file = new File(stopWordsFile2);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			// Read RM file line per line
			String line;

			while ((line = bufferedReader.readLine()) != null)
			{
				// Consider whitespace and - as separator.
				line = line.replace("-", " "); // est-ce = est + ce 
				line = line.replace("'", " "); // d'aller = d + aller
				String[] parts = line.split(" ");

				for(String word : parts) {
					int comment = 0;

					if (word.length() > 0) 
					{
						if(word.equals("|"))
						{
							// Where a in a comment section.
							comment = 1;
						}
						else if (comment == 0)
						{
							// Add word to stop word list.
							stopWordsList.add(word);
						}
					}			

				}
			}
			fileReader.close();
		}
	}


	/**
	 * Select the most efficient slave available to launch a job.
	 * 
	 * Launch the job.
	 * 
	 * @throws InterruptedException 
	 * 
	 */
	private static int launchJobs(List<Job> jobList) throws InterruptedException {
		float best = 0;
		SlaveExecutor bestSlave = null;
		
		// Going through each slave, find the most efficient one.
		for(SlaveExecutor slave : slaveList) {
			// A TERMINATED salve can take a new job.
			if(slave.getJobState() == State.TERMINATED) 
			{
				float current = slave.getScore();
				if(current >= best) {
					best = current;
					bestSlave = slave;
				}
			}
		}
		
		if(bestSlave == null) {
			logger.Log("No slave available");
			Thread.sleep(100);
			return 1;
		}
		
		if(bestSlave.getScore() == 0) {
			Thread.sleep(500);
		}
		
		// Go through jobList to find the job ready to start.
		int ret = assignJob(bestSlave, jobList);
		if(ret == -1) {
			// No more job to execute.
			return 0;
		}
		
		return 1;
	}

	/**
	 * Parse the list of job, and assign a not-started job to slave.
	 * 
	 * @param slave
	 * @return 		0 if a job has been assigned to the slave, 
	 * 				-1 if no more job is pending.
	 * @throws InterruptedException 
	 * 
	 */
	private static int assignJob(SlaveExecutor slave, List<Job> jobList) throws InterruptedException {

		// Slave: a machine
		int ret = -1;

		for(Job job : jobList) {
			// A 0 status means the job is terminated correctly. Otherwise we have to run it.
			if(job.getStatus() == JobStatus.NOT_STARTED) {
				logger.Log("Starting " + job.getJobDescription());
				slave.startJob(job);
				return 0;
			}

			if(job.getStatus() == JobStatus.STARTED) {
				// At least 1 remaining Job, need to sure it will finish properly.
				Thread.sleep(10);
				ret = 0;
			}
		}

		return ret;
	}

	/**
	 * 
	 * Args parser.
	 * 
	 * @param args
	 */
	private static void parseArgs(String[] args) {
		
		int mandatory = 0;
		
		if(args.length >= 6) {
			for (int i = 0; i < args.length; i += 2) {
				switch(args[i]) {
				case "-w":				// Working directory
				case "-workingdir":				// Working directory
					workingDir = args[i+1]+"/";
					mandatory++;
					break;
				case "-l":				// List of Slaves
				case "-slavelist":
					slaveListFile = args[i+1];
					mandatory++;
					break;
				case "-i":				// Input file
				case "-inputfile":
					inputFile = args[i+1]; 
					mandatory++;
					break;
				case "-s":				// Stop words list 
				case "-stopwordsfile":
					stopWordsFile = args[i+1]; 
					break;
				case "-r":				// number of reducer 
				case "-nbreducers":
					numberOfReducer = Integer.parseInt(args[i+1]); 
					break;
				case "-n":
				case "-nblines":
					numberOfLinePerFile = Integer.parseInt(args[i+1]); 
					break;
				default:
					throw new IllegalArgumentException(ARGS_ERROR);
				}	    	
			}
		}
		
		
		if(mandatory == 0)
		{
			throw new IllegalArgumentException(ARGS_ERROR);
		}
	}

}
