package org.brann.clock.fixtures;

import java.util.HashMap;
import java.util.Map;

import org.brann.clock.LogicalClock;

public class LogicalClockRelations {
	
	private static final String CLOCK2 = "clock2";
	private static final String CLOCK1 = "clock1";
	Map<String, org.brann.clock.LogicalClock> clocks;
	
	public LogicalClockRelations() {
		
		clocks = new HashMap<String, org.brann.clock.LogicalClock>();
		clocks.put(CLOCK1, new LogicalClock());
		clocks.put(CLOCK2, new LogicalClock());
	}
	
	
	public void setTick1( int ticks) {
		
		for (int i=0;i < ticks; ++i) {
			
			clocks.get(CLOCK1).tick();
		}
		return;
	}
	
	public void setTick2( int ticks) {
		
		for (int i=0;i < ticks; ++i) {
			
			clocks.get(CLOCK2).tick();
		}
		return;
	}
	
	
	public boolean clock1LtClock2() {
		return clocks.get(CLOCK1).isLessThan(clocks.get(CLOCK2));
	}

	public boolean clock2LtClock1() {
		return clocks.get(CLOCK2).isLessThan(clocks.get(CLOCK1));
	}

}
