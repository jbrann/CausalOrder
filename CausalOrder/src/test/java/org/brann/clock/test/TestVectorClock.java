package org.brann.clock.test;

import static org.junit.Assert.*;

import org.brann.clock.TextConstants;
import org.brann.clock.VectorClock;
import org.junit.Test;

public class TestVectorClock {


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

}
