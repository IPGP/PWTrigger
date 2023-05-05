/**
 * Created Feb 6, 2014 03:50:00 PM
 * Copyright 2014 Observatoire volcanologique du Piton de La Fournaise / IPGP
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
 * @author patriceboissier
 *
 */
public class WebLog {
	private File eventLogDir;
	private Logger appLogger;
	
	public WebLog(File eventLogDir, Logger appLogger) {
		this.eventLogDir = eventLogDir;
		this.appLogger = appLogger;
	}
	
	public void addEvent(String eventDescription) {
		System.out.println("New event to add to log : " + eventDescription);
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
            		System.out.println(cur+"\n");
            		writer.write(cur+"\n");
            	}
            	counter++;
            }
            writer.close();
        	
            //FileWriter out = new FileWriter(eventFile, true);
            //out.write(eventDescription + "\n");
            //out.close();
        } catch(IOException ioe) {
        	appLogger.error("Probleme d'ecriture : " + ioe);
        }
	}
}
