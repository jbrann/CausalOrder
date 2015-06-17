/**
 * 
 */
package org.brann.clock.test;

import static org.junit.Assert.*;

import org.brann.clock.VectorTimeStamp;
import org.junit.Test;

/**
 * @author f400670
 *
 */
public class TestVectorTimeStamp {

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
		
		// now round-trip the p2 clock to show the JSON works
		
		VectorTimeStamp p2copy = new VectorTimeStamp(null, p2.toString());
		assertTrue(p2.inCausalOrder(p2copy));
		assertTrue(p2copy.inCausalOrder(p2));
		
	}
	
	/**
	 * Test method for {@link org.brann.clock.VectorTimeStamp#VectorTimeStamp(String, String)}.
	 * to test various failure scenarios when rebuilding from JSON
	 */
	@Test
	public void testVectorTimeStampString() {
		
		VectorTimeStamp empty = new VectorTimeStamp("empty");
		
		VectorTimeStamp a = new VectorTimeStamp(null, "{\"O_PID\" : \"p2\",\"VC\" : [ {\"PID\" : \"p1\",\"LLC\" : [ 1 ]" +
				  "}, {\"PID\" : \"p2\",\"LLC\" : [ 2 ]} ],\"FC\" : [ {\"PID\" : \"p3\",\"VC\" : [ {\"PID\" : \"p1\"," +
				  "\"LLC\" : [ 1 ]}, {\"PID\" : \"p2\",\"LLC\" : [ 1 ]} ]} ]}");

		VectorTimeStamp b = new VectorTimeStamp(null, "{\"O_PID\" : \"p2\",\"VC\" : [ {\"PID\" : \"p1\",\"LLC\" : [ 1 ]" +
				  "}, {\"PID\" : \"p2\",\"LLC\" : [ 2 ]} ],\"FC\" : [ {\"PID\" : \"p3\",\"VC\" : [ {\"PID\" : \"p1\"," +
				  "\"LLC\" : [ 1 ]}, {\"PID\" : \"p2\",\"LLC\" : [ 1 ]} ]} ]}");

		// identical Vector timeStamps are in order (whichever order is tested)
		assertTrue(a.inCausalOrder(b));
		assertTrue(b.inCausalOrder(a));
		
		// exercise bad string content cases for:
		// O_PID, FC and PID
		
		a = new VectorTimeStamp(null, "{\"XXXX\" : \"p2\",\"VC\" : [ {\"PID\" : \"p1\",\"LLC\" : [ 1 ]" +
				  "}, {\"PID\" : \"p2\",\"LLC\" : [ 2 ]} ],\"FC\" : [ {\"PID\" : \"p3\",\"VC\" : [ {\"PID\" : \"p1\"," +
				  "\"LLC\" : [ 1 ]}, {\"PID\" : \"p2\",\"LLC\" : [ 1 ]} ]} ]}");
		
		assertTrue(a.inCausalOrder(empty));
		assertTrue(empty.inCausalOrder(a));
		
		a = new VectorTimeStamp(null, "{\"O_PID\" : \"p2\",\"VC\" : [ {\"PID\" : \"p1\",\"LLC\" : [ 1 ]" +
				  "}, {\"PID\" : \"p2\",\"LLC\" : [ 2 ]} ],\"XXXX\" : [ {\"PID\" : \"p3\",\"VC\" : [ {\"PID\" : \"p1\"," +
				  "\"LLC\" : [ 1 ]}, {\"PID\" : \"p2\",\"LLC\" : [ 1 ]} ]} ]}");	

		assertTrue(a.inCausalOrder(empty));
		assertTrue(empty.inCausalOrder(a));
		
		a = new VectorTimeStamp(null, "{\"O_PID\" : \"p2\",\"VC\" : [ {\"PID\" : \"p1\",\"LLC\" : [ 1 ]" +
				  "}, {\"PID\" : \"p2\",\"LLC\" : [ 2 ]} ],\"FC\" : [ {\"XXXX\" : \"p3\",\"VC\" : [ {\"PID\" : \"p1\"," +
				  "\"LLC\" : [ 1 ]}, {\"PID\" : \"p2\",\"LLC\" : [ 1 ]} ]} ]}");

		assertTrue(a.inCausalOrder(empty));
		assertTrue(empty.inCausalOrder(a));
		
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
		// checks that it is in order, then merges the foreign and local clocks
		p3.mergeLocal(p2p3);
		p3.mergeOther(p2p3);
		p3.tick();
		
//repeat to have pre-loaded timestamps		
		// p1 sends to p3:
				// send copy of p1 clock on message
				// then insert a foreign vector for p3 in p1's clock
				// tick p1
				p1p3 = new VectorTimeStamp(p1);
				p1.insertClockFor("p3");
				p1.tick();
								
				
				// p1 sends to p2:
				// send copy of p1 clock on message
				// then insert a foreign vector for p2 in p1's clock
				// tick p1
				p1p2 = new VectorTimeStamp(p1);
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
				p2p3 = new VectorTimeStamp(p2);
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
				// checks that it is in order, then merges the foreign and local clocks
				p3.mergeLocal(p2p3);
				p3.mergeOther(p2p3);
				p3.tick();
				
				//repeat to have pre-loaded timestamps		
				// p1 sends to p3:
						// send copy of p1 clock on message
						// then insert a foreign vector for p3 in p1's clock
						// tick p1
						p1p3 = new VectorTimeStamp(p1);
						p1.insertClockFor("p3");
						p1.tick();
										
						
						// p1 sends to p2:
						// send copy of p1 clock on message
						// then insert a foreign vector for p2 in p1's clock
						// tick p1
						p1p2 = new VectorTimeStamp(p1);
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
						p2p3 = new VectorTimeStamp(p2);
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
						// checks that it is in order, then merges the foreign and local clocks
						p3.mergeLocal(p2p3);
						p3.mergeOther(p2p3);
						p3.tick();
		
	}

}
