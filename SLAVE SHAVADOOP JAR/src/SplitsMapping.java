import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;


public class SplitsMapping {
	
	private String inFileName;
	private String outFileName;

	public SplitsMapping(String inFilePath, String outFilePath) {
		// A text file to "split"
		this.inFileName = inFilePath;
		// Result file
		this.outFileName = outFilePath;
	}
	
	/**
	 * @throws InterruptedException 
	 * 
	 */
	
	
	public void splitsMap() throws InterruptedException
	{
		try {
			// Open SPLIT_x file
			File file = new File(inFileName);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			// We will read the file line per line
			String line;
			// Create a dictionary word -> count
			Map<String, Integer> dictionary = new TreeMap<String, Integer>();

			System.out.println("Generating " + outFileName);
			
			// Read SPLIT_X file line per line
			while ((line = bufferedReader.readLine()) != null) {

				// Consider whitespace and - as separator.
				line = line.replace("-", " "); // est-ce = est + ce 
				line = line.replace("'", " "); // d'aller = d + aller
				String[] parts = line.split(" ");
				
				for(String word : parts) {
					if (word.length() > 0) {
						// Remove special character (most of them)
						String wordClean  = word.replaceAll("[,.():;?!«»*]", "");
						wordClean = wordClean.replace("\"", "");
						// We can use space as a delimiter as it has been removed from the string.
						wordClean = wordClean.toLowerCase();
						// 
						if(dictionary.containsKey(wordClean)) {
							Integer val = (Integer) dictionary.get(wordClean);
							dictionary.put(wordClean, val + 1);
						}
						else
							dictionary.put(wordClean, 1);		
					}			

				}
			}
			// Create output file (text for debug
			PrintWriter writer = new PrintWriter(outFileName+".txt");
			for (String key : dictionary.keySet()) {
				writer.println(key + " " + dictionary.get(key));
			}

			writer.close();
			fileReader.close();
			
			Thread.sleep(4000);

			
			// Create output file
			File file1 = new File(outFileName+".bin");
	        FileOutputStream f = new FileOutputStream(file1);
	        ObjectOutputStream s = new ObjectOutputStream(f);
	        s.writeObject(dictionary);
	        s.close();

		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	


}
