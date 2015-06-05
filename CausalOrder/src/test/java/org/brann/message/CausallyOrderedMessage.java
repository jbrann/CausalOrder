/* Generated by Together */

package org.brann.message;

import org.brann.clock.VectorTimeStamp;
import java.io.Serializable;

/**
 * A message passed between processes using a VectorTimeStamp to control
 * Causal Order.
 * @author John Brann 
 */
public class CausallyOrderedMessage implements Serializable {
    /**
     * obtain the VectorTimeStamp associated with this message
     * @see org.brann.clock.VectorTimeStamp
     * @return The timestamp of the message*/
    public VectorTimeStamp getTimestamp() {
        return new VectorTimeStamp(null, timestamp);
    }

    /**
     * Obtain the message payload.
     * @return The payload of the message 
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * @param The message payload
     * @param The timestamp of the process creating the message 
     */
    protected CausallyOrderedMessage(Object payload, String clock) {
        timestamp = clock;
        this.payload = payload;
    }

    private Object payload;
    private String timestamp;
}
