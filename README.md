# GcmFcmSender
Simple java CLI tool to send GCM/FCM push messages from local machine

File "api_key.txt" must be in project's root directory (or the same directory where executable .jar is). It must contain Server API key for sending push notifications (must be on 1st line exactly).

File msg.txt is an example of JSON message file. See more at https://firebase.google.com/docs/cloud-messaging/concept-options


# Usage:

	usage:
	 -d <arg>           Data payload ("message" parameter)
	 -f,--file <arg>    Path to message payload file in JSON format (ignores
	                    -d, -n and --token options)
	    --fcm           Work with FCM
	    --gcm           Work with GCM (default)
	 -h,--help          Show help
	 -n <arg>           Notification body
	 -t,--token <arg>   Device token of recipient
	
	Specify a test message to broadcast via GCM or FCM. If a device's registration token is
	specified, the message will only be sent to that device. Otherwise, the message 
	will be sent to all devices subscribed to the "global" topic.
	
	Example (Broadcast): 
	java -jar sender.jar -d "<Data Message>" -n "<Notification Body>"
	java -jar sender.jar --fcm --file "<Path>"