/**
 * Created May 5, 2023 by Patrice Boissier
 * Copyright 2023 Observatoire volcanologique du Piton de La Fournaise / IPGP
 */
package fr.ipgp.pwtrigger.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.util.Collections;
import java.util.ArrayList;
import org.apache.logging.log4j.Logger;


/**
 * This class is used to log events to a file for web display
 * @author patriceboissier
 */
public class WebLog {
	private File eventLogDir;
	private Logger appLogger;

	/**
	 * Constructor
	 * @param eventLogDir the directory where the event log file is stored
	 * @param appLogger the application logger
	 */
	public WebLog(File eventLogDir, Logger appLogger) {
		this.eventLogDir = eventLogDir;
		this.appLogger = appLogger;
	}
	
	/**
	 * Add an event to the event log file
	 * @param eventDescription the event description
	 */
	public void addEvent(String eventDescription) {
		appLogger.info("New event to add to log : " + eventDescription);
        File eventFile = new File(eventLogDir,"events.txt");
        try {
        	ArrayList<String> rows = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new FileReader(eventFile));

            String s;
            while((s = reader.readLine())!=null)
            	rows.add(s);
            reader.close();
            rows.add(eventDescription);
            
            Collections.sort(rows, Collections.reverseOrder());

            FileWriter writer = new FileWriter(eventFile);
            int counter = 0;
            for(String cur: rows) {
            	if(counter<=20) {
            		writer.write(cur+"\n");
            	}
            	counter++;
            }
            writer.close();
        	
        } catch(IOException ioe) {
        	appLogger.error("Write error : " + ioe);
        }
	}
}
