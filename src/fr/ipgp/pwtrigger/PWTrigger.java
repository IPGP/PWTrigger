/**
 * Created May 5, 2023 by Patrice Boissier
 * Copyright 2008 Observatoire volcanologique du Piton de La Fournaise / IPGP
 * 
 * Running :
 * java -Dlog4j2.configurationFile=file:./resources/log4j2.xml -jar PWTrigger.jar
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
	private static int checkInterval = 10;
	private static int timeWindow;
	private static int eventNumber;
	private static double eventMagnitudeMin;
	private static String fdsnwsURL;
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
		
		initConfiguration();

		appLogger.debug("Entering application.");
		
		while (true) {
			
			readConfiguration();
			
			appLogger.debug("Looking for events with parameters (time window/event numbers/min magnitude) : " 
					+ timeWindow + "/" + eventNumber + "/" + eventMagnitudeMin);
			
			appLogger.debug("##################### New iteration");

			checkEvents();

			cleanQueue(newEvents);
			cleanQueue(oldEvents);

			checkQueues();
			
			try {
				Thread.sleep(1000 * checkInterval);
			} catch (InterruptedException ie) {
				appLogger.error("Error while sleeping!");
			}
		}
		
	}
	
	/**
	 * Initialize the configuration.
	 * Create ReloadingFileBasedConfigurationBuilder object and start the reloading trigger
	 */
	private static void initConfiguration() {
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
	
	/**
	 * Reads the configuration file and initialize the variables
	 */
	private static void readConfiguration() {
		try {
		    configuration = builder.getConfiguration();
		    eventLogDir = new File(configuration.getString("triggers.event_log_dir"));
		    if (!eventLogDir.exists()) {
		    	appLogger.fatal("Event log directory does not exist. Exiting application");
		    	System.exit(1);
		    }
		    timeWindow = configuration.getInt("alarm.time_window");
			eventNumber = configuration.getInt("alarm.event_number");
			eventMagnitudeMin = configuration.getDouble("alarm.event_magnitude_min");
			fdsnwsURL = configuration.getString("fdsnws.url");
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
	
	/**
	 * Extracts events from FDSNWS based on criterias (time window; magnitude).
	 * Checks if the events are already in the newEvents queue or oldEvents queue.
	 * If not, adds the event to the newEvents queue.
	 * 
	 */
	private static void checkEvents() {
		Date endTime = new Date();
		Date startTime = new Date(endTime.getTime() - timeWindow * 60 * 1000);
		// Make FDSNWS request
		ServiceUtil serviceUtil = ServiceUtil.getInstance();
		serviceUtil.setAppName("PWTrigger");
		EventService eventService = serviceUtil.getEventService(fdsnwsURL);
		EventCriteria criteria = new EventCriteria();
		// Extract events in time window based on the following criterias.
		criteria.setStartTime(startTime);
		criteria.setEndTime(endTime);
		criteria.setMinimumMagnitude(eventMagnitudeMin);
		try {
			List<Event> events = eventService.fetch(criteria);
			for (Event event : events) {
				// Create EventWrapper object to compare events
				Boolean alreadyUsed = false;
				EventWrapper eventWrapper = new EventWrapper(event);

				for (EventWrapper newEventWrapper : newEvents) {
					if (eventWrapper.equals(newEventWrapper)) {
						alreadyUsed = true;
					}
				}

				for (EventWrapper oldEventWrapper : oldEvents) {
					if (eventWrapper.equals(oldEventWrapper)) {
						alreadyUsed = true;
					}
				}

				if (alreadyUsed) {
					appLogger.debug("Event already used : " + eventWrapper.getEvent().getPublicId());
				} else {
					appLogger.debug("New event : " + eventWrapper.getEvent().getPublicId());
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
	
	/**
	 * Clean queue from events older than time window
	 * @param pbq PriorityBlockingQueue to clean
	 * @throws ConcurrentModificationException if the queue is modified while iterating
	 */
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
	
	/**
	 * Check if the number of events in the newEvents queue is greater than the eventNumber threshold.
	 * If so, send a trigger and transfer the events to the oldEvents queue.
	 */
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
	
	/**
	 * Send a trigger to the alarm server
	 */
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
