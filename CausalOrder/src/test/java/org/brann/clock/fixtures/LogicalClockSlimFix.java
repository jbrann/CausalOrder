package org.brann.clock.fixtures;

import java.util.HashMap;
import java.util.Map;

public class LogicalClockSlimFix {
	
	Map<String, org.brann.clock.LogicalClock> clocks = new HashMap<String, org.brann.clock.LogicalClock>();
	
	public String setClock(String name) {
		clocks.put(name, new org.brann.clock.LogicalClock());
		return name;
	}
	
	public boolean clockExists(String name) {
		return clocks.containsKey(name);
	}

}
