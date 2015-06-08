/**
 * 
 */
package org.brann.clock.test;

import static org.junit.Assert.*;

import org.brann.clock.LogicalClock;
import org.brann.clock.TextConstants;
import org.junit.Test;

/**
 * @author f400670
 *
 */
public class TestLogicalClock {

	/**
	 * Test method for {@link org.brann.clock.ClockOperations#tick()}.
	 */
	@Test
	public void testTickIsLessThan() {
		
		LogicalClock a = new LogicalClock();
		LogicalClock b = new LogicalClock();
		
		assertFalse(a.isLessThan(a));
		
		assertFalse(a.isLessThan(b));
		assertFalse(b.isLessThan(a));
		
		a.tick();
		assertTrue(b.isLessThan(a));
		assertFalse(a.isLessThan(b));
		
		a.tick();
		assertTrue(b.isLessThan(a));
		assertFalse(a.isLessThan(b));
		
		b.tick();
		assertTrue(b.isLessThan(a));
		assertFalse(a.isLessThan(b));
		
		b.tick();
		assertFalse(a.isLessThan(b));
		assertFalse(b.isLessThan(a));
		
		b.tick();
		assertTrue(a.isLessThan(b));
		assertFalse(b.isLessThan(a));
		
		assertFalse(a.isLessThan(a));
	}

	
	/**
	 * Test method for {@link org.brann.clock.LogicalClock#toJson(com.fasterxml.jackson.core.JsonGenerator)}.
	 */
	@Test
	public void testToFromJson() {
		
		LogicalClock a = new LogicalClock();
		for (int i=0;i<100;++i) a.tick();
		
		String aAsJson = a.toString();
		LogicalClock b = new LogicalClock(aAsJson);
		
		assertFalse(a.isLessThan(b));
		assertFalse(b.isLessThan(a));
		
		a = new LogicalClock("{\""+TextConstants.LAMPORT_LOGICAL_CLOCK+"\":["+Integer.MAX_VALUE+","+Integer.MAX_VALUE+"]}");
		b = new LogicalClock("{\""+TextConstants.LAMPORT_LOGICAL_CLOCK+"\":["+Integer.MAX_VALUE+","+Integer.MAX_VALUE+"]}");
		
		assertFalse(a.isLessThan(b));
		assertFalse(b.isLessThan(a));
		
		b.tick();
		assertTrue(a.isLessThan(b));
		assertFalse(b.isLessThan(a));
		
		a.tick();
		a.tick();
		assertFalse(a.isLessThan(b));
		assertTrue(b.isLessThan(a));
		
		a = new LogicalClock("not good json");
		b = new LogicalClock();
		
		assertFalse(a.isLessThan(b));
		assertFalse(b.isLessThan(a));
		
		
	}
	
	/**
	 * Test method for {@link org.brann.clock.LogicalClock#clone()}.
	 */
	@Test
	public void testLogicalClockLogicalClock() {

		LogicalClock a = new LogicalClock("{\""+TextConstants.LAMPORT_LOGICAL_CLOCK+"\":["+Integer.MAX_VALUE+","+Integer.MAX_VALUE+"]}");
		LogicalClock clone = new LogicalClock(a);
		
		assertFalse(a.isLessThan(clone));
		assertFalse(clone.isLessThan(a));
		
	}

}
