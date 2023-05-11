/**
 * Created May 5, 2023 by Patrice Boissier
 * Copyright 2023 Observatoire volcanologique du Piton de La Fournaise / IPGP
 */
package fr.ipgp.pwtrigger.event;

import edu.iris.dmc.event.model.Event;
/**
 * This class is a wrapper for the seismic events.<br/>
 * It is used to sort the events by time and publicId.<br/>
 * <br/>
 * @author patriceboissier
 */
public class EventWrapper implements Comparable<EventWrapper> {
	private Event event;

	/**
	 * Constructor
	 * @param event the seismic event
	 */
	public EventWrapper(Event event) {
		this.event = event;
	}

	/**
	 * @return the seismic event
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
	 * @param o the EventWrapper object to compare to the EventWrapper
	 * @return returns a negative integer, 0, or a positive integer depending on whether the receiving object is less than, equal to, or greater than the specified object.
	 * @throws ClassCastException if the specified object's type prevents it from being compared to this Object.
	 */
	public int compareTo(EventWrapper o) {
		EventWrapper eventWrapper = (EventWrapper)o;
        int lastCmp = this.event.getPreferredOrigin().getTime().compareTo(eventWrapper.getEvent().getPreferredOrigin().getTime());
        return (lastCmp != 0 ? lastCmp : this.event.getPublicId().compareTo(eventWrapper.getEvent().getPublicId()));
    }
	
	/**
	 * Indicates whether some other object is "equal to" this Comparator. This method must obey the general contract of Object.equals(Object)
	 * Overrides : equals in class Object
	 * @param o the EventWrapper object to compare to the EventWrapper
	 * @return true only if the specified object is also a comparator and it imposes the same ordering as this comparator
	 */
    public boolean equals(EventWrapper o) {
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
