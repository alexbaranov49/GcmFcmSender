package by.abaranov.gcmsender;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;


public class FcmGcmSender {
//	public static final String API_KEY = "AIzaSyAmN4KGNTV3a3bG1vPwLoXskEi5CeoSrFs";
	public static final String API_KEY_FILEPATH = "api_key.txt";
	public static final String SERVER_GCM = "https://android.googleapis.com/gcm/send";
	public static final String SERVER_FCM = "https://fcm.googleapis.com/fcm/send";
	
	private static final String OPT_DATA_MESSAGE = "d";
	private static final String OPT_NOTIFICATION_MESSAGE = "n";
	private static final String OPT_TOKEN_SHORT = "t";
	private static final String OPT_TOKEN_LONG = "token";
	private static final String OPT_HELP_SHORT = "h";
	private static final String OPT_HELP_LONG = "help";
	private static final String OPT_FCM = "fcm";
	private static final String OPT_GCM = "gcm";
	private static final String OPT_FILE_SHORT = "f";
	private static final String OPT_FILE_LONG = "file";
	
	public static void main(String[] args) {
		System.out.println("Working Directory = " +
	              System.getProperty("user.dir"));
		Options options = createOptions();
		CommandLine parsedArgs = parseArgs(args, options);		
		if (! optionsValid(parsedArgs) || parsedArgs.hasOption(OPT_HELP_SHORT)) {
			showHelp(options);
        }        
		
        try {
        	String apiKey = null;
        	apiKey = readApiKey(API_KEY_FILEPATH);
        	if (isEmpty(apiKey)) {
        		System.exit(3);
        	}
        	
        	byte[] payload = null;
            if (parsedArgs.hasOption(OPT_FILE_SHORT)) {
            	payload = readPayloadFromFile(parsedArgs.getOptionValue(OPT_FILE_SHORT));
            	if (payload == null) {
            		System.exit(2); // abort
            	}
            } else {
            	payload = createPayloadFromParameters(parsedArgs);
            }
        	
        	String serverURL = SERVER_GCM;
        	if (parsedArgs.hasOption(OPT_FCM)) {
    			serverURL = SERVER_FCM;
    		}
            // Create connection to send GCM Message request.
            URL url = new URL(serverURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "key=" + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Send GCM message content.
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(payload);

            // Read GCM response.
            InputStream inputStream = conn.getInputStream();
            String resp = IOUtils.toString(inputStream);
            System.out.println(resp);
            System.out.println("Check your device/emulator for notification or logcat for " +
                    "confirmation of the receipt of the GCM message.");
        } catch (IOException e) {
            System.out.println("Unable to send GCM message.");
            System.out.println("Please ensure that API_KEY has been replaced by the server " +
                    "API key, and that the device's registration token is correct (if specified).");
            e.printStackTrace();
        }
	}
	
	private static String readApiKey(String path) {	
		Path resultPath = Paths.get(path);		
		try {			
			List<String> lines = Files.readAllLines(resultPath, Charset.defaultCharset());
			if (lines != null && lines.size() > 0 && ! isEmpty(lines.get(0))) {
				return lines.get(0);
			} else {
				System.err.println("No api key provided! Please add it in first line of " + API_KEY_FILEPATH);
				return null;
			}
		} catch (IOException e) {
			System.err.println("Could not read from " + resultPath.toAbsolutePath().toString() + ". Is it missing?");
			e.printStackTrace();
			return null;
		}
	}
		
	private static byte[] readPayloadFromFile(String filePath) {
		if (! isEmpty(filePath)) {
			try {
				byte[] encoded = Files.readAllBytes(Paths.get(filePath));
				return encoded;
			} catch (IOException e) {
				System.err.println("Error reading from file!");
				e.printStackTrace();				
				return null;
			}
		} else {
			System.err.println("No file path specified!");
			return null;
		}
	}
	
	private static byte[] createPayloadFromParameters(CommandLine parsedArgs) {
		String dataMessage = parsedArgs.getOptionValue(OPT_DATA_MESSAGE);
		String notificationMessage = parsedArgs.getOptionValue(OPT_NOTIFICATION_MESSAGE);
		String deviceToken = parsedArgs.getOptionValue(OPT_TOKEN_LONG);				
		
		JSONObject jGcmData = new JSONObject();
		try {
			// Prepare JSON containing the GCM message content. What to send and where to send.	    	
	    	JSONObject jData = null;
	    	if (dataMessage != null) {
	        	jData = new JSONObject();
	            if (dataMessage != null) {
	            	jData.put("message", dataMessage);
	            }
	    	}
	    	
	    	JSONObject jNotification = null;
	    	if (notificationMessage != null) {
	        	jNotification = new JSONObject();
	            if (dataMessage != null) {
	            	jData.put("body", notificationMessage);
	            	jData.put("title", "Notification title");
	            	jData.put("icon", "icon");
	            }
	    	}
	        
	    	// Where to send GCM message.
	        if (! isEmpty(deviceToken)) {
	            jGcmData.put("to", deviceToken);
	        } else {
	            jGcmData.put("to", "/topics/global");
	        }
	        
	        // What to send in GCM message.
	        if (jData != null) {
	        	jGcmData.put("data", jData);
	        }
	        if (jNotification != null) {
	        	jNotification.put("notification", jData);
	        }
		} catch (JSONException ex) {
        	System.out.println("Erroneous JSON.");
        	ex.printStackTrace();
        }
		
		return jGcmData.toString().getBytes();
	}
	
	private static boolean optionsValid(CommandLine args) {
		if (args == null) {
			System.err.println("Null arguments!");
			return false;
		}
		if (!args.hasOption(OPT_DATA_MESSAGE) && !args.hasOption(OPT_NOTIFICATION_MESSAGE) && !args.hasOption(OPT_FILE_LONG)) {
			System.err.println("Need to specify message content! (-d, -n or --file options) \n");
			return false;
		}	
		return true;
	}
	
	private static CommandLine parseArgs(String[] args, Options opt) {
		DefaultParser parser = new DefaultParser();
		try {
			return parser.parse(opt, args);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static Options createOptions() {		
		Options options = new Options();
		options.addOption(OPT_FILE_SHORT, OPT_FILE_LONG, true, "Path to message payload file in JSON format (ignores -d, -n and --token options)");
		options.addOption(OPT_HELP_SHORT, OPT_HELP_LONG, false, "Show help");
		options.addOption(OPT_DATA_MESSAGE, true, "Data payload (\"message\" parameter)");
		options.addOption(OPT_NOTIFICATION_MESSAGE, true, "Notification body");
		options.addOption(OPT_TOKEN_SHORT, OPT_TOKEN_LONG, true, "Device token of recipient");
		options.addOption(null, OPT_FCM, false, "Work with FCM");
		options.addOption(null, OPT_GCM, false, "Work with GCM (default)");
		return options;
	}
	
	private static void showHelp(Options options) {
		HelpFormatter help = new HelpFormatter();
		help.printHelp("gcmSender", options);
        System.out.println("\nSpecify a test message to broadcast via GCM or FCM. If a device's registration token is\n" +
                "specified, the message will only be sent to that device. Otherwise, the message \n" +
                "will be sent to all devices subscribed to the \"global\" topic.");
        System.out.println("");
        System.out.println("Example (Broadcast): -d \"<Data Message>\" -n \"<Notification Body>");
        System.out.println("");
        System.exit(1);
	}
	
	private static boolean isEmpty(String s) {
		return (s == null || "".equals(s)); 
	}

}
