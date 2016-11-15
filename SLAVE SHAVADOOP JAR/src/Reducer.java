import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

public class Reducer {

	private String inFileName;
	private String outFileName;

	/**
	 * Simple constructor to initialize input and output files.
	 * 
	 * @param inFilePath	file to reduce (Sorted Map)
	 * @param outFilePath	reduced file (Reduced Map)
	 */
	public Reducer(String inFilePath, String outFilePath) {
		// A text file to reduce
		this.inFileName = inFilePath;
		// Result file
		this.outFileName = outFilePath;
	}
	
	/**
	 * Transforms Sorted Map with multiple keys to Reduced Map with unique keys.
	 * adroitement 1, afin 3, agacé 2, ... , adversaire 1, afin 4, agacé 1
	 * will become
	 * adroitement 1, afin 4, agacé 3, ... , adversaire 1
	 * 
	 * @throws InterruptedException
	 */
	public void reduce() throws InterruptedException
	{
		try {
			// Open UM_x file
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

				// line will look like adroitement 2
				String[] parts = line.split(" ");
				String key = parts[0]; // adroitement
				Integer val = (Integer) Integer.parseInt(parts[1]); // 2

				// adroitement 4 can be already in the list
				if(dictionary.containsKey(key)) {
					val = dictionary.get(key) + val;
				}
				
				dictionary.put(key, val);	
			}
			
			// Create output file (text for debug)
			PrintWriter writer = new PrintWriter(outFileName+".txt");
			for (String key : dictionary.keySet()) {
				writer.println(key + " " + dictionary.get(key));
			}

			writer.close();
			fileReader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
