/**
 * Created May 9, 2008 2:26:12 PM
 * Copyright 2008 Observatoire volcanologique du Piton de La Fournaise / IPGP
 */
package fr.ipgp.pwtrigger;

import fr.ipgp.pwtrigger.utils.CommonUtilities;
import fr.ipgp.pwtrigger.utils.WebLog;
import fr.ipgp.pwtrigger.alarm.*;
import fr.ipgp.pwtrigger.event.EventWrapper;
import java.io.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import edu.iris.dmc.criteria.CriteriaException;
import edu.iris.dmc.criteria.EventCriteria;
import edu.iris.dmc.service.ServiceUtil;
import edu.iris.dmc.service.EventService;
import edu.iris.dmc.service.NoDataFoundException;
import edu.iris.dmc.service.ServiceNotSupportedException;
import edu.iris.dmc.event.model.Event;
import edu.iris.dmc.event.model.Magnitude;


/**
 * Entry point of the application
 * @author Patrice Boissier
 */
public class PWTrigger {
	public static XMLConfiguration configuration;
	public static ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builder;
	public static Logger appLogger = LogManager.getLogger("PWTrigger");
	private static String configurationFileName;
	private static File eventLogDir;
	private static int parseInterval = 10;
	private static int timeWindow;
	private static int eventNumber;
	private static InetAddress inetAddress;
	private static int port;
	private static boolean createTrigger;
	private static int priority;
	private static String confirmCode;
	private static String callList;
	private static String warningMessage;
	private static boolean repeat;
	private static PriorityBlockingQueue<EventWrapper> newEvents = new PriorityBlockingQueue<EventWrapper>();
	private static PriorityBlockingQueue<EventWrapper> oldEvents = new PriorityBlockingQueue<EventWrapper>();
	private static TriggerSender triggerSender;


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length > 0 && args[0].equals("--stop")) {
			if (SystemUtils.IS_OS_LINUX) {
				try {
					String myName = ManagementFactory.getRuntimeMXBean().getName();
					System.out.println("My name : " + myName);
					String line;
					Process p = Runtime.getRuntime().exec("ps -ef");
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((line = input.readLine()) != null) {
						String lineElements[] = line.split("\\s+");
						String processID = lineElements[1];
						String processName = lineElements[7];
						for (int i = 8; i<lineElements.length; i++) {
							processName += " " + lineElements[i];
						}
						if (processName.startsWith("java -jar PWTrigger.jar --config ")) {
							System.out.println("Killing process with PID : " + processID);
							Runtime.getRuntime().exec("kill -9 "+ processID);
						}
					}
					input.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				} finally {
					System.exit(1);
				}
			}
			if (SystemUtils.IS_OS_WINDOWS) {
				
			}
		}
		
		if (args.length == 2 && args[0].equals("--config")) {
			File configurationFile = new File(args[1]);
			if (configurationFile.exists()) {
				configurationFileName = args[1];
			} else {
				appLogger.fatal("Fatal error : configuration file not present or not readable. Exiting application");
				System.exit(1);
			}
		} else {
			configurationFileName = "resources/pwtrigger.xml";
		}
		
		checkUnicity();
		
		readConfiguration();

		appLogger.debug("Entering application.");
		
		while (true) {
			
			reloadConfiguration();
			
			appLogger.debug("Looking for events with parameters (time window/event numbers/station number) : " 
					+ timeWindow + "/" + eventNumber);
			
			appLogger.debug("##################### New iteration");

			checkEvents();

			cleanQueue(newEvents);
			cleanQueue(oldEvents);				
			checkQueues();
			
			try {
				Thread.sleep(1000 * parseInterval);
			} catch (InterruptedException ie) {
				appLogger.error("Error while sleeping!");
			}
		}
		
	}
	
	/**
	 * Reads XML configuration file and creates a XMLConfiguration object
	 * The application log a fatal error and exists if the configuration file is missing
	 */
	private static void readConfiguration() {
		//Configurations configurations = new Configurations();
		Parameters params = new Parameters();
		File configurationFile = new File(configurationFileName);
		builder = 
			new ReloadingFileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
			.configure(params.fileBased().setFile(configurationFile));
		PeriodicReloadingTrigger trigger = new PeriodicReloadingTrigger(builder.getReloadingController(),
			null, 10, TimeUnit.SECONDS);
		trigger.start();
	}
	
	private static void reloadConfiguration() {
		try {
		    configuration = builder.getConfiguration();
		    eventLogDir = new File(configuration.getString("triggers.event_log_dir"));
		    if (!eventLogDir.exists()) {
		    	appLogger.fatal("Event log directory does not exist. Exiting application");
		    	System.exit(1);
		    }
		    timeWindow = configuration.getInt("alarm.time_window");
			eventNumber = configuration.getInt("alarm.event_number");
			inetAddress =  InetAddress.getByName(configuration.getString("triggers.host"));
			port = configuration.getInt("triggers.port");
			createTrigger = configuration.getBoolean("triggers.create_triggers");
			priority = configuration.getInt("triggers.priority");
			confirmCode = configuration.getString("triggers.confirm_code");
			callList = configuration.getString("triggers.call_list");
			warningMessage = configuration.getString("triggers.warning_message");
			repeat = configuration.getBoolean("triggers.repeat");
		} catch(ConfigurationException cex) {
			appLogger.fatal("Fatal error : configuration file not present or not readable. Exiting application");
			System.exit(1);
		} catch(UnknownHostException uhe) {
			appLogger.fatal("Fatal error : unknown host in configuration file : " + uhe.getMessage() + ". Exiting application");
			System.exit(1);
		}
	}
	
	private static void checkEvents() {
		Date endTime = new Date();
		Date startTime = new Date(endTime.getTime() - timeWindow * 60 * 1000);
		// Make FDSNWS request
		ServiceUtil serviceUtil = ServiceUtil.getInstance();
		serviceUtil.setAppName("PWTrigger");
		EventService eventService = serviceUtil.getEventService("http://195.83.188.34:8080/fdsnws/event/1/");
		EventCriteria criteria = new EventCriteria();
		// Extract events in time window
		criteria.setStartTime(startTime);
		criteria.setEndTime(endTime);
		// TODO : add magnitude criteria in configuration file
		criteria.setMinimumMagnitude(0.5);
		try {
			List<Event> events = eventService.fetch(criteria);
			for (Event event : events) {
				//for(Magnitude magnitude:event.getMagnitudes()){
				//	System.out.printf("\tMag: %3.1f %s\n", magnitude.getValue(), magnitude.getType());
				//}
				EventWrapper eventWrapper = new EventWrapper(event);
				if (newEvents.contains((EventWrapper)eventWrapper) || oldEvents.contains((EventWrapper)eventWrapper)) {
					appLogger.debug("Event already used.");
				} else {
					appLogger.debug("New event.");
					newEvents.add(eventWrapper);
					WebLog webLog = new WebLog(eventLogDir, appLogger);
					webLog.addEvent(event.toString());
				}
			}
		} catch (NoDataFoundException ndfe) {
			appLogger.warn("No events found in time window : " + ndfe.getMessage());
		} catch (CriteriaException ce) {
			appLogger.warn("Problem with criteria : " + ce.getMessage());
		} catch (IOException ioe) {
			appLogger.warn("Problem with event log : " + ioe.getMessage());
		} catch (ServiceNotSupportedException snse) {
			appLogger.warn("Service not supported : " + snse.getMessage());
		}
    }
	
	private static void cleanQueue(PriorityBlockingQueue<EventWrapper> pbq) throws ConcurrentModificationException {
		ArrayList<EventWrapper> toBeRemoved = new ArrayList<EventWrapper>();
		for(EventWrapper e : pbq) {
			Date now = new Date();
			if (e.getEvent().getPreferredOrigin().getTime().getTime() < (now.getTime()-(timeWindow*60*1000))) {
				toBeRemoved.add(e);
			}
		}
		for(EventWrapper e : toBeRemoved) {
			pbq.remove(e);
		}
	}
	
	private static void checkQueues() {
		appLogger.debug(newEvents.size() + " events in the new events queue and " + oldEvents.size() + " events in the old events queue.");
		if (newEvents.size() >= eventNumber) {
			appLogger.info("Event threshold reached !");
			if (createTrigger)
				sendTrigger();
			ArrayList<EventWrapper> toBeTransfered = new ArrayList<EventWrapper>();
			for(EventWrapper e : newEvents) {
				oldEvents.add(e);
				toBeTransfered.add(e);
			}
			for(EventWrapper e : toBeTransfered) {
				newEvents.remove(e);
			}
		}
	}
	
	private static void sendTrigger() {
		triggerSender = new TriggerSender(inetAddress, port);
		triggerSender.send(priority, confirmCode, callList, warningMessage, repeat);
		appLogger.info("Trigger sent.");
	}
	
	/**
	 * Check unicity of the application
	 */
	private static void checkUnicity() {
		try {
			if (!CommonUtilities.appIsUnique("PWTrigger")) {
				appLogger.fatal("Application already running : exiting");
				System.exit(1);
			}
		} catch (FileNotFoundException fnfe) {
			appLogger.warn("Unable to create lock file to ensure unicity of the application");
		} catch (IOException ioe) {
			appLogger.warn("Unable to set lock file to ensure unicity of the application");
		}
	}
}
