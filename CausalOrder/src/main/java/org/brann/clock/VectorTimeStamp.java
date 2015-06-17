package org.brann.clock;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

/**
* A Vector Time Stamp is a collection of Vector Clocks used in
* determining the Causal Order of message processing.
* <P> The contents are an owner - the name of the process using
* this timestamp, the owner's Vector Clock and a collection of
* Vector Clocks representing the known condition of the Vector
* Clocks of other processes
*/
@SuppressWarnings("serial")
public class VectorTimeStamp implements Serializable {
    
	/**
	 * produce JSON in the argument generator from this object
	 * does not write start or end object (must be managed by caller)
	 * @param jg - the JSON Generator class that will produce the output
	 * @throws IOException 
	 * @throws JsonGenerationException 
	 */
	
	protected void toJson(JsonGenerator jg) throws JsonGenerationException, IOException {
		
		jg.writeStringField(TextConstants.OWNER_PROCESS_ID, owner);
		myclock.toJson(jg);

		jg.writeFieldName(TextConstants.FOREIGN_CLOCKS);
		jg.writeStartArray();
		
		for (String clockOwner : stamp.keySet()) {
        	jg.writeStartObject();
        	
        	jg.writeStringField(TextConstants.PROCESS_ID, clockOwner);
        	
        	stamp.get(clockOwner).toJson(jg);
        	
        	jg.writeEndObject();
        }		
		jg.writeEndArray();
	}
	
	/** 
	 * Produce computer and human-readable output.
	 * 
	 *  allows the vector to be recreated from the output string.
	 *  
	 */
	@Override
    public String toString() {
    	
    	StringWriter sw = new StringWriter();
    	
    	try {
			JsonGenerator jg = new JsonFactory().createGenerator(sw);
			jg.setPrettyPrinter(new com.fasterxml.jackson.core.util.DefaultPrettyPrinter());
			
			
			jg.writeStartObject();
			
			toJson(jg);
			
			jg.writeEndObject();
			jg.flush();
			jg.close();
		} catch (IOException e) {
			e.printStackTrace();
		}    	

    	
    	return sw.toString();
    }

    /**
    * Implement the Schiper Eggli Sandoz protocol for
    * determining causal order of point-to-point messages.
    *
    * <P>A received message is IN causal order if the timestamp
    * on the received message contains no vector clock for this
    * process OR the vector clock for this process in the received
    * timestamp is less than the receiving process's own vector clock
    */
    public synchronized boolean inCausalOrder(VectorTimeStamp received) {
        
        VectorClock receivedClock = received.stamp.get(owner);
        
        if (receivedClock != null) {
            if (!(receivedClock.lessThan(myclock))) {
                return false;
            }
        }
        return true;
    }

    /**
    * recognize an event at this process.  The effect is to increment the
    * Logical clock for the owning process in the owning process's vector
    * clock
    */
    public synchronized void tick() {
        myclock.tick(owner);
    }

    /**
     * Tick the destination member of this process's vector clock.
     * Used in multicast extension to SES
     * @param destn
     */
    public synchronized void tick(String destn) {
        myclock.tick(destn);
    }

    /**
    * Set the Vector Clock for the named process to the current value of the
    * owner's clock.  Any existing clock is over-written.
    */
    public synchronized void insertClockFor(String other) {

        stamp.put(other, (VectorClock)myclock.clone());
    }
    
    /**
     * Multicast extension - set sender's clock in the various receiver vector clocks (so the multicast
     * receivers are updated for each other's reception of the message)
     * 
     * @param senderClock
     */
    public synchronized void setForSender(VectorTimeStamp senderClock) {
        myclock.setClockFor (senderClock.owner, 
                senderClock.myclock.clockFor(senderClock.owner));
    }

    /**
    * Merge with a received vector timestamp.  The algorithm of merging is as
    * follows:
    * <P> For each foreign Vector Clock in the received timestamp, merge with the
    * corresponding foreign vector Clock in this timestamp.  If this timestamp did not contain
    * a Vector Clock for the process concerned, insert the received Vector Clock
    * into this timestamp. [Except where the process is this process.]
    *
    */
    public synchronized void mergeOther(VectorTimeStamp other ) {

        ;

        for (String wknm : other.stamp.keySet()) {
             
             if (wknm.compareTo(owner) != 0) {
                if (stamp.containsKey(wknm)) {
                    stamp.get(wknm).merge(other.stamp.get(wknm));
                        // received clock for a process is less than my own - do nothing
                } else {
                   stamp.put (wknm, (VectorClock)other.stamp.get(wknm));
                }
             }
        }
    }

    /**
    * Merge Local clock with a received vector timestamp's local clock.  
    */
    public synchronized void mergeLocal(VectorTimeStamp other ) {

        myclock.merge(other.myclock);
    }

    /**
    * Create a new timestamp for the named process
    */
    public VectorTimeStamp(String name) {
        owner = name;
        myclock = new VectorClock();
        stamp = new HashMap<String, VectorClock>();
    }

    /**
    * create a new timestamp identical to this one.
    * Used when building a timestamp to put on an outgoing message.
    */
    public VectorTimeStamp(VectorTimeStamp source) {

            owner = source.owner;
            myclock = (VectorClock)source.myclock.clone();
            stamp = new HashMap<String, VectorClock>();
            synchronized (source.stamp) {
                for (String key : source.stamp.keySet()) {
                	stamp.put(key, ((VectorClock)(source.stamp.get(key)).clone()));
                }                     
            }
    }
    
    protected void fromJson(JsonParser jp) throws IOException {
    	
		while (jp.nextToken() != JsonToken.END_OBJECT) { // first pass moves past start of object

			if (jp.getCurrentName().compareTo(TextConstants.OWNER_PROCESS_ID) != 0)
				throw new JsonParseException("Unexpected field name: "+jp.getCurrentName()+" should be: "+ TextConstants.OWNER_PROCESS_ID, 
						jp.getCurrentLocation());
			jp.nextToken();
			owner = jp.getText();
			
			jp.nextToken();
			myclock = new VectorClock(); 
			myclock.fromJson(jp);
			
			if (jp.getCurrentName().compareTo(TextConstants.FOREIGN_CLOCKS) != 0)
				throw new JsonParseException("Unexpected field name: "+jp.getCurrentName()+" should be: "+ TextConstants.FOREIGN_CLOCKS, 
						jp.getCurrentLocation());
			jp.nextToken();
						
			while (jp.nextToken() != JsonToken.END_ARRAY) {
	
				jp.nextToken(); // past start object
				
				if (jp.getCurrentName().compareTo(TextConstants.PROCESS_ID) != 0)
					throw new JsonParseException("Unexpected field name: "+jp.getCurrentName()+" should be: "+ TextConstants.LAMPORT_LOGICAL_CLOCK, 
							jp.getCurrentLocation());
				
				jp.nextToken(); // to process name
				String pid = jp.getValueAsString();
				
				jp.nextToken();
				VectorClock vc = new VectorClock();
				vc.fromJson(jp);
				
				stamp.put(pid, vc);
//				jp.nextToken(); // past end object				
			}
		}

    	
    }
    
    /**
     * build a Vector time stamp from the owner name and a String produced by the toString method
     * @param name name of the owner, if Null, the owner from the stringStamp is used
     * @param stringStamp representation of Vector clock produced by toString operation of this class
     */
    public VectorTimeStamp(String name, String stringStamp) {
    	
        stamp = new HashMap<String, VectorClock>();
        
        // re-assemble a vector time stamp from the argument
        
    	try {
			JsonParser jp = new JsonFactory().createParser(stringStamp);

			jp.nextToken(); //move to start object
			
			fromJson(jp);
		
			jp.close();
		} catch (IOException e) {
			// can't happen in Stringreader - create null stamp
			stamp = new HashMap<String, VectorClock>();
			myclock = new VectorClock();
		}
    	
    	// correct ownership if the argument owner does not match the restored stamp
    	// clock for owner must be "zero'd"
    	
    	if (name != null) {
    		owner = name;
    	    myclock = new VectorClock();
    	}
    		    	
    }

    private VectorClock myclock;
    private String owner;
    private Map<String, VectorClock> stamp;

}
