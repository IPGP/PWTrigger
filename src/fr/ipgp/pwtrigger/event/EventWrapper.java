/**
 * Created May 9, 2008 2:26:12 PM
 * Copyright 2008 Observatoire volcanologique du Piton de La Fournaise / IPGP
 */
package fr.ipgp.pwtrigger.event;

import java.util.*;
import java.text.*;

import edu.iris.dmc.event.model.Event;
/**
 * This class represent a seismic event as seen by the EarthWorm system.<br/>
 * <br/>
 * @author patriceboissier
 */
public class EventWrapper implements Comparable {
	private Event event;

	public EventWrapper(Event event) {
		this.event = event;
	}

	/**
	 * @return the event
	 */
	public Event getEvent() {
		return event;
	}
	
	/**
	 * @return a string representation of the object
	 */
	public String toString() {
		return this.event.toString();
	}
	
	/**
	 * The compareTo method compares the receiving object with the specified object.
	 * If the specified object cannot be compared to the receiving object, the method 
	 * throws a ClassCastException.
	 * @param o the Object to compare to the Event
	 * @return returns a negative integer, 0, or a positive integer depending on whether the receiving object is less than, equal to, or greater than the specified object.
	 * @throws ClassCastException - if the specified object's type prevents it from being compared to this Object.
	 */
	public int compareTo(Object o) {
		EventWrapper eventWrapper = (EventWrapper)o;
        int lastCmp = this.event.getPreferredOrigin().getTime().compareTo(eventWrapper.getEvent().getPreferredOrigin().getTime());
        return (lastCmp != 0 ? lastCmp : this.event.getPublicId().compareTo(eventWrapper.getEvent().getPublicId()));
    }
	
	/**
	 * Indicates whether some other object is "equal to" this Comparator. This method must obey the general contract of Object.equals(Object)
	 * Overrides : equals in class Object
	 * @param o the Object to compare to the Event
	 * @return true only if the specified object is also a comparator and it imposes the same ordering as this comparator
	 */
    public boolean equals(Object o) {
        if (!(o instanceof EventWrapper))
            return false;
        EventWrapper eventWrapper = (EventWrapper)o;
        return eventWrapper.getEvent().getPublicId().equals(this.event.getPublicId());
    }
    
	/**
	 * Returns a hash code value for the object. This method is supported for the benefit of hashtables such as those provided by java.util.Hashtable.
	 * Overrides : hashCode in class Object
	 * @return the hash code value for the object
	 */
    public int hashCode() {
        return this.event.getPublicId().hashCode();
    }
}
