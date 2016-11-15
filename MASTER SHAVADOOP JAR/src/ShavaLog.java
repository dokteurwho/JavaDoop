import java.text.SimpleDateFormat;
import java.util.Date;

public class ShavaLog {
	
	private String nameClass;
	SimpleDateFormat sdf;
	

	public ShavaLog(String name) {
		nameClass = name;
		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	}
	
	public void Log(String message) {
		
		Date date = new Date();
		String fullLog = sdf.format(date) + "," + nameClass + "," + message;
		System.out.println(fullLog);		
	}

}
