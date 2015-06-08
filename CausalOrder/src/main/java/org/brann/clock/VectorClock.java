package org.brann.clock;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.Iterator;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

/**
 * Implementation of the Vector Clock extension to
 *
 * Lamport's Logical Clocks.
 *
 * A VectorClock is a set of name/value pairs where the
 * name identifies a process in a distributed environment
 * and the value is a LogicalClock.
 *
 * @author John Brann
 * @see LogicalClock
 * @type Product Requirement
 */

@SuppressWarnings("serial")
public class VectorClock extends ClockOperations implements Serializable, Cloneable {
    
    static final LogicalClock zero = new LogicalClock();
	private Map<String, LogicalClock> clocks = new TreeMap<String, LogicalClock>();
	
	
	protected VectorClock() { // used in clone()
    	
    }
    
    /**
     * Extract from the provided JsonParser.
     * {"VC":[{"PID":"foo","LLC":[1]}]}
     * 
     * Parser should point at the START_OBJECT for the Vector Clock's representation
     * will be returned at the END_OBJECT
     * @param jp
     * @throws IOException 
     * @throws JsonParseException 
     */
    protected void fromJson(JsonParser jp) throws IOException {
    	
    	clocks = new HashMap<String, LogicalClock>();
    	

		if (jp.getCurrentName().compareTo(TextConstants.VECTOR_CLOCK) != 0)
			throw new JsonParseException("Unexpected field name: "+jp.getCurrentName()+" should be: "+ TextConstants.VECTOR_CLOCK, 
					jp.getCurrentLocation());
		
		// start of Array;
		for (jp.nextToken(); 
			 jp.nextToken() != JsonToken.END_ARRAY;) {

			jp.nextToken(); // past start object
			
			if (jp.getCurrentName().compareTo(TextConstants.PROCESS_ID) != 0)
				throw new JsonParseException("Unexpected field name: "+jp.getCurrentName()+" should be: "+ TextConstants.PROCESS_ID, 
						jp.getCurrentLocation());
			
			jp.nextToken(); //move to pid name
			String pid = jp.getValueAsString();
			jp.nextToken(); 
			LogicalClock tempClock = new LogicalClock();
			tempClock.fromJson(jp);
			clocks.put(pid, tempClock);
//				jp.nextToken(); // past end object
		}
		jp.nextToken();  // past the end-array

    	    	
    }
    
    /**
     * Constructor to build a VectorClock from the human-readable String created by toString()
     * 
     * 
     * @param stringValue the JSON string value of the vector clock
     */
    public VectorClock(String stringValue) {
    	
    	try {
			JsonParser jp = new JsonFactory().createParser(stringValue);

			jp.nextToken(); //move to start object
			jp.nextToken(); // move past start object
			
			fromJson(jp);
			
			jp.close();
		} catch (IOException e) {
			// swallow the exception,
			// set clock empty - failed to build it.
			clocks = new TreeMap<String, LogicalClock>();
		}   	
    }
    
    
    /**
     * Update this Vector Clock with the values found in the argument.
     * Vector Clocks are merged according to the following algorithm:
     * For each key name in the argument clock:
     *    if this Vector does not contain the key, insert the key
     *       and a copy of the Logical Clock in this Vector.
     *    else
     *       update the value for the key in this vector to be the
     *       greater of the argument value and the current value.
     */
    
    public synchronized void merge(VectorClock other) {
        
        for (Iterator<String> otherIt = other.clocks.keySet().iterator();
        otherIt.hasNext();) {
            String nm = otherIt.next();
            
            if (!clocks.containsKey(nm) ||
                ((LogicalClock)clocks.get(nm)).isLessThan(
                		(LogicalClock)other.clocks.get(nm))) {
                    clocks.put(nm, (LogicalClock) (other.clocks.get(nm)).clone());
            }
        }
    }
    
    protected synchronized LogicalClock clockFor (String name) {
        return (LogicalClock)clocks.get(name);
    }
    
    protected synchronized void setClockFor (String name, LogicalClock clock) {
        clocks.put (name, (LogicalClock) clock.clone());
    }
    
     /**
      * create JSON in the provided JsonGenerator 
      *
     * @throws IOException 
     * @throws JsonGenerationException */
    
    protected void toJson(JsonGenerator jg) throws JsonGenerationException, IOException {
    	
	
		jg.writeArrayFieldStart(TextConstants.VECTOR_CLOCK);
		
		for (Iterator <String> prnenum = clocks.keySet().iterator();
		        prnenum.hasNext();) {
			
			jg.writeStartObject();
			String pid = prnenum.next();
			jg.writeStringField(TextConstants.PROCESS_ID, pid);
			clocks.get(pid).toJson(jg);
			jg.writeEndObject();
		}
		jg.writeEndArray();

    }
    
    
    /**
     * provide human and computer-readable JSON output.  
     * format is:  {"VC":[{"PID":<process id>,<Logical Clock>},...]}
     * 
     */
    @Override
    public String toString() {
    	
    	StringWriter sw = new StringWriter();
    	
    	try {
			JsonGenerator jg = new JsonFactory().createGenerator(sw);
			jg.setPrettyPrinter(new DefaultPrettyPrinter());
			
			jg.writeStartObject();
			
			toJson(jg);
			
			jg.writeEndObject();
			jg.flush();
			jg.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	

    	
    	return sw.toString();
    }
      
    
    
    /**
     * Create a new, separate copy of the Vector Clock, identical to the
     * original
     */
    @Override
    public Object clone() {
        String wknm;
        
        VectorClock temp = new VectorClock();
        
        for (Iterator<String> myIt = this.clocks.keySet().iterator();
             myIt.hasNext();) {
            
            wknm = myIt.next();
            temp.clocks.put(wknm,
            (LogicalClock) ((LogicalClock)this.clocks.get(wknm)).clone());
        }
        return temp;
    }
    
    /**
     * Logical Relation.  This VectorClock is less than the parameter VectorClock if
     * all of the following conditions are fulfilled:
     *
     * 1. No LogicalClock in the other vector is less than the corresponding
     * LogicalClock in this Vector
     * 2. At least one LogicalClock in this vector is less than the
     * corresponding LogicalClock in the other vector.
     *
     * For the purposes of comparison a missing key in either vector has a
     * Logical Clock value of zero (never ticked)
     * @return true if this is less than the parameter, false otherwise
     * @param VectorClock to compare with
     */
    public boolean lessThan(VectorClock other) {
        
        return this.lessThan((ClockOperations) other);
    }
     
    /**
     * Determine if the argument Vectore Clock is "Less than" this one
     * 
     * In order for THIS Vector Clock to be less than another, all of the following must be true:
     * for every Logical Clock in common, none of the clocks in THIS vector can be greater than its counterpart
     * for every Logical Clock in common, at least one in THIS must be less than its counterpart
     * for every Logical Clock in THIS but absent from the other, the value in THIS must be zero.
     */
    @Override
    protected boolean isLessThan(ClockOperations other) {
        
        LogicalClock wk;
        LogicalClock otherWk;
        String nm;
        boolean lessThan = false;
        
        for (Iterator<String> MyIt = clocks.keySet().iterator();
        MyIt.hasNext();) {
            
            nm = MyIt.next();
            wk = clocks.get(nm);
            otherWk = ((VectorClock)other).clocks.get(nm);
            
            if (otherWk != null) {   // present in Compare clock
            
                // If the foreign key is less than mine,
                // I cannot be < the other clock
                if (otherWk.isLessThan(wk))
                    return false;
                if (!lessThan)
                    lessThan = wk.isLessThan(otherWk);
            } else {
                // not present in foreign clock, compare my value with zero
                if (zero.isLessThan(wk)) {
                    return false;
                }
            }
        }
        
        // At this point we have tested all the Logical Clocks in
        // my vector.  We can no longer find a clock in my vector that
        // is greater than the equivalent in other.  Therefore, if we are less than the
        // other by the comparison to this point, we are done.
        // If we have not found a 'less than' clock yet, we must search for a
        // clock in the other vector, that we do not have, that is greater than zero.
        
        if (!lessThan) {
            for (Iterator<String> otherIt = ((VectorClock)other).clocks.keySet().iterator();
                 !lessThan &&
                 otherIt.hasNext();) {
            
                nm = otherIt.next();
            
                if (!(clocks.containsKey(nm))) {
                    otherWk = ((VectorClock)other).clocks.get(nm);
                    if (zero.isLessThan(otherWk))
                        lessThan = true;
                }
            }
        }
        return lessThan;
    }
    
    /**
     * Increment the Logical Clock in this Vector belonging to the
     * argument key.
     */
    public void tick(String name) {
        LogicalClock toTick = clocks.get(name);
        
        if (toTick == null) {
            clocks.put (name, (toTick = new LogicalClock()));
        }
        
        toTick.tick();
    }
    
    // no-op - required by abstract base class
    @Override
    protected void doTick() {
    }
}
