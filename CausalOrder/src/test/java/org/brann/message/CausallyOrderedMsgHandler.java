/* Generated by Together */

package org.brann.message;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

import org.brann.clock.VectorTimeStamp;

/**
 * A helper class that manages the control of causally ordered point-to-point messages.
 * The Schiper-Eggli-Sandoz protocol is used to implement the ordering.
 * 
 * Message payloads to be sent are properly timestamped and received messages (with timestamps)
 * are checked for causal order correctness and delivered (or buffered) as appropriate.
 * @author John Brann
 * @see VectorClock 
 */
public class CausallyOrderedMsgHandler {

    private String name;
	public String getName() {
		return name;
	}

	private LinkedList<CausallyOrderedMessage> heldMessages;
	private VectorTimeStamp clock;
	/**
     * Buffer an out-of-order message 
     */
    private void holdMessage(CausallyOrderedMessage msg) {
        heldMessages.add(msg);
    }

    /**
     * Check a message received from the transport for causal order.
     * If in order check for buffered messages that can now be delivered.
     * @param received message with timestamp
     * @return Vector of message payloads that can now be processed (null if parameter message is out of order) 
     */
    public List<Object> recvMessage(CausallyOrderedMessage msg) {
        LinkedList<Object> results = null;
        
        if (checkOrderAndReceive(msg)) {
            
            results = new LinkedList<Object>();
            results.add(msg.getPayload());
            if (heldMessages.size() > 0) {
                results.addAll(scanHeld());
            }
        } else {
            holdMessage(msg);
        }
        return results;
    }

    private boolean checkOrderAndReceive(CausallyOrderedMessage msg) {
        
    	VectorTimeStamp fromMsg = new VectorTimeStamp(null, msg.getTimestamp());
    	
        if (clock.inCausalOrder(fromMsg)) {

            synchronized (clock) {
                clock.mergeLocal(fromMsg);
                clock.mergeOther(fromMsg);
                clock.tick();
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * iterate over held messages, releasing those that are freed up by 
     * previously received messages.
     */
    private List<Object> scanHeld() {
        
        LinkedList<Object> delivered = new LinkedList<Object>();
        boolean deliveredThisPass;
        
        do {
            deliveredThisPass = false;
            for (ListIterator<CausallyOrderedMessage> lIt = heldMessages.listIterator();
                 lIt.hasNext();) {

                CausallyOrderedMessage next = (CausallyOrderedMessage)lIt.next();
                
                if (checkOrderAndReceive(next)) {
                    lIt.remove();
                    delivered.add(next.getPayload());
                    deliveredThisPass = true;
                }
            }
        } while (heldMessages.size() > 0 && deliveredThisPass);
        
        return delivered;
    }           

    /**
     * Send a message to a single destination
     * @return message with appropriate timestamp
     * @param payload to deliver
     * @param Name of destination process 
     */
    public CausallyOrderedMessage sendMessage(Object payload, String destn) {

        CausallyOrderedMessage result;
        
        synchronized (clock) {
            
            result = new CausallyOrderedMessage(payload, clock.toString());
            clock.insertClockFor(destn);
            clock.tick();
        }
        
        return result;
    }

    /**
     * Multicast a message to a number of destinations.
     * @return array of messages, each with an appropriate timestamp.
     * @param message payload
     * @param array of destination process names 
     */
    public CausallyOrderedMessage[] sendMessage(Object payload, String[] destn) {
        VectorTimeStamp tmp;
        CausallyOrderedMessage result[] =
            new CausallyOrderedMessage[destn.length];

        synchronized (clock) {

            for (int dests = 0;
                 dests < destn.length;
                 ++dests) {
                    tmp = new VectorTimeStamp(clock);
                    for (int inloop = 0;
                         inloop < destn.length;
                         ++inloop)
                        if (dests!= inloop)
                           tmp.insertClockFor(destn[inloop]);

                    result[dests] = new CausallyOrderedMessage (payload, tmp.toString());
            }
            for (int dests = 0;
                 dests < destn.length;
                 ++dests) {
                    clock.insertClockFor(destn[dests]);
                    clock.tick(destn[dests]);
            }
            clock.tick();
        }
        return result;
    }

    /**
     * Creates a message handler for the named process
     * @param Name of the process handling messages 
     */
    public CausallyOrderedMsgHandler(String name) {
        this.name = name;
        heldMessages = new LinkedList<CausallyOrderedMessage>();
        clock = new VectorTimeStamp(name);
    }

    public VectorTimeStamp getClock() {
		return clock;
	}
}
