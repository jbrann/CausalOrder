/*
 * ClockOperations.java
 *
 * Created on November 18, 2002, 1:12 PM
 */

package org.brann.clock;

/**
 *
 * @author  jbrann
 */
@SuppressWarnings("serial")
abstract class ClockOperations implements java.io.Serializable, Cloneable {
    
    private static final Object emergencyLock = new Object();
    
    private static boolean emergencyMutex = false;
    
    /** Creates a new instance of ClockOperations */
    ClockOperations() {
    }
    
    public synchronized void tick() {
        this.doTick();
    }
    
    protected abstract boolean isLessThan (ClockOperations other);
    
    protected boolean lessThan (ClockOperations other) {
        
        boolean emergency = false;
        boolean result;
        ClockOperations firstLock;
        ClockOperations secondLock;
        
        int myHash = System.identityHashCode(this);
        int otherHash = System.identityHashCode(other);
        
        if (myHash < otherHash) {
            firstLock = this;
            secondLock = other;
        } else if (myHash > otherHash) {
            firstLock = other;
            secondLock = this;
        } else { //Uh-oh, equal hash codes.  If these are actually the same object
                 // there is no problem.  Otherwise there's a risk of deadlock here.
            if (this.equals(other)) {
                return false;  // can't be less than self
            }
            synchronized (emergencyLock) {
                while (emergencyMutex) {
                    try {
                        emergencyLock.wait();
                    } catch (InterruptedException ie) {}
                }
                emergencyMutex = true;
                firstLock = this;
                secondLock = other;
            }
        }
        synchronized (firstLock) {
            synchronized (secondLock) {
                
                result = this.isLessThan(other);
            }
        }
        if (emergency) {
            synchronized (emergencyLock) {
                emergencyMutex = false;
                emergencyLock.notifyAll();
            }
        }
        return result;        
    }
    
    protected abstract void doTick();
    
}
