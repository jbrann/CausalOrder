package org.brann.clock;

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
 * A LogicalClock is an expandable, positive valued integer. It consists of an
 * array of ints, each of which represents a 'digit' in the number. When the
 * existing digit overflows, another is added.
 *
 * the array is "BigEndian" - the highest index to the array is the lowest
 * value.
 * 
 * add lines to get a change
 * 
 * @author John Brann
 */
@SuppressWarnings("serial")
public class LogicalClock extends ClockOperations implements Serializable {

	/** the 'digits' */
	private int[] value;

	/**
	 * Creates an uninitialized value.
	 */
	public LogicalClock() {
		value = new int[1];
	}

	/**
	 * Constructor that builds a LogicalClock from a JSON String. The String is
	 * the form that was created by the "toString()" Method - i.e. a complete
	 * 
	 * @param value
	 */
	public LogicalClock(String stringValue) {

		value = new int[1];

		try {
			JsonParser jp = new JsonFactory().createParser(stringValue);

			jp.nextToken(); // move to start object
			jp.nextToken(); // move past start object

			fromJson(jp);

			jp.close();
		} catch (IOException e) {
			// Failed to parse, clean up the clock and make it zero.
			this.value = new int[1];
		}

	}

	/**
	 * new LogicalClock is created identical to the argument value
	 */
	public LogicalClock(LogicalClock source) {
		this.value = new int[source.value.length];
		System.arraycopy(source.value, 0, this.value, 0, source.value.length);
	}

	/**
	 * Create JSON in the provided generator object. this produces a snippet -
	 * the caller is responsible for starting and ending the object and managing
	 * the generator.
	 *
	 * @throws IOException
	 * @throws JsonGenerationException
	 */

	protected void toJson(JsonGenerator jg) throws IOException {

		jg.writeArrayFieldStart(TextConstants.LAMPORT_LOGICAL_CLOCK);
		for (int i = 0; i < value.length; ++i)
			jg.writeNumber(value[i]);
		jg.writeEndArray();

	}

	/**
	 * produce JSON representing the content of the Logical Clock This operation
	 * produces a complete JSON object that can be a part of a larger JSON
	 * representation (e.g. a Vector Clock) or stand alone.
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
			// can't happen when writing to String (unless we are out of memory
			// which will be fatal

		}
		return sw.toString();
	}

	/**
	 * Parse JSON into the provided parser. The parser should be pointing at the
	 * field name for the LogicalClock on return it points at the END_OBJECT or
	 * the LLC
	 * 
	 * @param jp
	 * @throws IOException
	 * @throws JsonParseException
	 */
	protected void fromJson(JsonParser jp) throws IOException {

		if (jp.getCurrentName().compareTo(TextConstants.LAMPORT_LOGICAL_CLOCK) != 0)
			throw new JsonParseException("Unexpected field name: "
					+ jp.getCurrentName() + " should be: "
					+ TextConstants.LAMPORT_LOGICAL_CLOCK,
					jp.getCurrentLocation());

		jp.nextToken(); // start of Array

		for (boolean first = true; jp.nextToken() != JsonToken.END_ARRAY;) {

			if (!first) {
				int[] newValue = new int[value.length + 1];
				System.arraycopy(value, 0, newValue, 0, value.length);
				value = newValue;
			}
			value[value.length - 1] = jp.getIntValue();
			first = false;
		}
		jp.nextToken(); // past end array
	}

	/**
	 * tick the value, expanding the number of 'digits' as necessary.
	 */
	@Override
	protected void doTick() {

		int counter;

		for (counter = value.length - 1; counter >= 0; --counter) {

			if (value[counter] < java.lang.Integer.MAX_VALUE) {
				// tick the 'digit' and break out of the loop.
				++value[counter];
				return;
			} else
				// overflow - move on to next 'digit'
				value[counter] = 0;
		}

		// total overflow, add a 'digit'
		int[] newval = new int[value.length + 1];
		newval[0] = 1;
		value = newval;
	}

	public boolean isLessThan(LogicalClock other) {
		return this.lessThan((ClockOperations) other);
	}

	/**
	 * Logical comparison. A clock is less than another when it has been ticked
	 * fewer times.
	 * 
	 * @returns true if this is less than the argument, false otherwise.
	 */
	@Override
	protected boolean isLessThan(ClockOperations other) {

		boolean result = false;

		if (this.value.length == ((LogicalClock) other).value.length) {

			// if lengths are the same, work from the high-value down, testing each
			for (int counter = 0; counter < this.value.length; ++counter) {
				if (this.value[counter] != ((LogicalClock) other).value[counter]) {
					result = (this.value[counter] < ((LogicalClock) other).value[counter]);
					break;
				}
			}
		} else if (this.value.length < ((LogicalClock) other).value.length) {
			// if this is shorter, it's smaller
			result = true;
		} else {
			// if this is longer, it's larger
			result = false;
		}
		return result;
	}

}
