package org.brann.clock.test;

import static org.junit.Assert.*;

import org.brann.clock.LogicalClock;
import org.brann.clock.TextConstants;
import org.brann.clock.VectorClock;
import org.brann.clock.VectorTimeStamp;
import org.junit.Test;

public class TestAllCausalOrder {

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

	@Test
	public void testMerge() {
		
		VectorClock foo = initVc();	
		VectorClock bar = initVc();
        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));
		
		foo.tick("foo");  // bar now less than foo
        assertFalse(foo.lessThan(bar));
        assertTrue(bar.lessThan(foo));

        bar.tick("bar"); // now simultaneous
        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));
        
        foo.tick("quux");  
        bar.tick("quux");
        bar.tick("quux");  //still simultaneous, but now bar has ticked more quux entries, foo more foo
        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));

        foo.merge(bar);  // after the merge, bar is less again
        assertFalse(foo.lessThan(bar));
        assertTrue(bar.lessThan(foo));

        bar.tick("foo");  // bar catches up with foo, simultaneous again
        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));
        
	}

	@Test
	public void testToStringFromString() {
		
		VectorClock foo = initVc();
        
		foo.tick("foo");
		foo.tick("foo");
		foo.tick("foo");

		VectorClock bar = new VectorClock(foo.toString());
        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));

        foo.tick("foo");
        assertFalse(foo.lessThan(bar));
        assertTrue(bar.lessThan(foo));
        
        bar.tick("foo");
        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));
		
	}

	private VectorClock initVc() {
		VectorClock foo = new VectorClock ("{\""+
        		TextConstants.VECTOR_CLOCK+"\":[{\""+TextConstants.PROCESS_ID+"\":\"foo\",\""+
        		TextConstants.LAMPORT_LOGICAL_CLOCK+"\":[1]}]}");
		return foo;
	}

	@Test
	public void testClone() {
		
		VectorClock foo = initVc();
        
		VectorClock bar = (VectorClock)foo.clone();
        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));

        foo.tick("foo");
        assertFalse(foo.lessThan(bar));
        assertTrue(bar.lessThan(foo));
        
        bar.tick("bar");
        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));

	}

	@Test
	public void testTickStringLessThan() {

        VectorClock foo = initVc();
        
        VectorClock bar = new VectorClock(foo.toString());
        
        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));
             
        foo.tick("foo"); // bar is now less than foo
        assertFalse(foo.lessThan(bar));
        assertTrue(bar.lessThan(foo));
        
        bar.tick("bar"); // now foo and bar are simultaneous, neither is ahead of the other
        bar.tick("bar");

        assertFalse(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));
        
        bar.tick("foo");  // foo is now less than bar
        
        assertTrue(foo.lessThan(bar));
        assertFalse(bar.lessThan(foo));
	}


	/**
	 * Test method for {@link org.brann.clock.VectorTimeStamp#inCausalOrder(org.brann.clock.VectorTimeStamp)}.
	 */
	@Test
	public void testInCausalOrder() {
	
		// Schiper Eggli Sandoz protocol test
		// p1 sends to p3
		// p1 sends to p2
		// p2 receives p1's message
		// p2 sends to p3
		// p3 receives p2's message (out of order)
		// p3 receives p1's message (in order) and can deliver p2's message
		
		VectorTimeStamp p1 = new VectorTimeStamp("p1");
		VectorTimeStamp p2 = new VectorTimeStamp("p2");
		VectorTimeStamp p3 = new VectorTimeStamp("p3");
	
		// p1 sends to p3:
		// send copy of p1 clock on message
		// then insert a foreign vector for p3 in p1's clock
		// tick p1
		VectorTimeStamp p1p3 = new VectorTimeStamp(p1);
		p1.insertClockFor("p3");
		p1.tick();
						
		
		// p1 sends to p2:
		// send copy of p1 clock on message
		// then insert a foreign vector for p2 in p1's clock
		// tick p1
		VectorTimeStamp p1p2 = new VectorTimeStamp(p1);
		p1.insertClockFor("p2");
		p1.tick();
		
		// p2 receives p1's message:
		// checks that it is in order, then merges the foreign and local clocks
		assertTrue(p2.inCausalOrder(p1p2));
		p2.mergeLocal(p1p2);
		p2.mergeOther(p1p2);
		p2.tick();
		
		// p2 sends to p3:
		// send copy of p2 clock on message
		// then insert a foreign vector for p3 in p2's clock
		// tick p2
		VectorTimeStamp p2p3 = new VectorTimeStamp(p2);
		p2.insertClockFor("p3");
		p2.tick();
		
		// p3 receives p2's message - out of order
		assertFalse(p3.inCausalOrder(p2p3));
		
		// p3 receives p1's message:
		// checks that it is in order, then merges the foreign and local clocks
		assertTrue(p3.inCausalOrder(p1p3));
		p3.mergeLocal(p1p3);
		p3.mergeOther(p1p3);
		p3.tick();
		
		// p3 now can receive p2's message - it is in order
		assertTrue(p3.inCausalOrder(p2p3));
		
		
		
	}


	/**
	 * Test method for {@link org.brann.clock.VectorTimeStamp#toString()}.
	 */
	@Test
	public void testToString() {
	
		VectorTimeStamp p1 = new VectorTimeStamp("p1");
		VectorTimeStamp p2 = new VectorTimeStamp("p2");
		VectorTimeStamp p3 = new VectorTimeStamp("p3");
	
		// p1 sends to p3:
		// send copy of p1 clock on message
		// then insert a foreign vector for p3 in p1's clock
		// tick p1
		VectorTimeStamp p1p3 = new VectorTimeStamp(p1);
		p1.insertClockFor("p3");
		p1.tick();
						
		
		// p1 sends to p2:
		// send copy of p1 clock on message
		// then insert a foreign vector for p2 in p1's clock
		// tick p1
		VectorTimeStamp p1p2 = new VectorTimeStamp(p1);
		p1.insertClockFor("p2");
		p1.tick();
		
		// p2 receives p1's message:
		// checks that it is in order, then merges the foreign and local clocks
		p2.mergeLocal(p1p2);
		p2.mergeOther(p1p2);
		p2.tick();
		
		// p2 sends to p3:
		// send copy of p2 clock on message
		// then insert a foreign vector for p3 in p2's clock
		// tick p2
		VectorTimeStamp p2p3 = new VectorTimeStamp(p2);
		p2.insertClockFor("p3");
		p2.tick();
		
		// p3 receives p1's message:
		// checks that it is in order, then merges the foreign and local clocks
		p3.mergeLocal(p1p3);
		p3.mergeOther(p1p3);
		p3.tick();
		
		// p3 now can receive p2's message - it is in order
		p3.mergeLocal(p2p3);
		p3.mergeOther(p2p3);
		p3.tick();
		
		// now round-trip the p3 clock to show the JSON works
		
		VectorTimeStamp p3copy = new VectorTimeStamp(null, p3.toString());
		assertTrue(p3.inCausalOrder(p3copy));
		assertTrue(p3copy.inCausalOrder(p3));
		
		String json = p1.toString();
	}

}
