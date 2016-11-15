import java.io.IOException;
import java.net.InetAddress;


public class Slave {

	private enum Mode {
		NA,
		SPLIT_MAPPING,
		REDUCING
	}

	public Slave() {
		// TODO Auto-generated constructor stub
	}


	public static void main(String[] args) throws InterruptedException {

		Mode mode;
		String inFileName = "";
		String outFileName = "";

		// Parse command line. We expect: slave.jar -m filename -o output file name
		if(args.length == 4) {
			switch(args[0]) {
			case "-m":
				mode = Mode.SPLIT_MAPPING;
				inFileName = args[1];
				break;
			case "-r":
				mode = Mode.REDUCING;
				inFileName = args[1];
				break;
			default:
				throw new IllegalArgumentException("Expected -m filename -o output file name");
			}
			switch(args[2]) {
			case "-o":
				outFileName = args[3];
				break;
			default:
				throw new IllegalArgumentException("Expected -m filename -o output file name");
			}
		}
		else
		{
			throw new IllegalArgumentException("Expected exe -m filename -o output_file_name");
		}

		// TODO Auto-generated method stub
		try {


			InetAddress addr;
			addr = InetAddress.getLocalHost();
			String hostname = addr.getHostName();
			
			if(mode == Mode.SPLIT_MAPPING) {
				SplitsMapping mapper = new SplitsMapping(inFileName, outFileName);

				System.out.print("[" + addr + "] Slave counting key from Sx file " + inFileName + "...");
				mapper.splitsMap();
				System.out.print("... job finished. Output UMx file " + outFileName);	
			}
			else {

				Reducer reducer = new Reducer(inFileName, outFileName);
				
				System.out.print("[" + addr + "] Slave reducing keys from SMx file " + inFileName + "...");
				reducer.reduce();
				System.out.print("... job finished. Output RMx file " + outFileName);	
				
			}


		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}


}
