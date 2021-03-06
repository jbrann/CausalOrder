package org.brann.clock.test;

import java.util.*;
import java.io.*;

import org.brann.clock.VectorTimeStamp;
import org.brann.message.*;

public class Testbed {
	public static void main(String[] args) {
		HashMap<String, CausallyOrderedMsgHandler> processes = new HashMap<String, CausallyOrderedMsgHandler>();
		HashMap<String, CausallyOrderedMessage> messages = new HashMap<String, CausallyOrderedMessage>();

		long b4 = System.currentTimeMillis();

		if (args.length < 1) {
			System.out.println("Usage: <program> file");
			System.exit(1);
		}

		try {
			FileReader fr = new FileReader(args[0]);
			BufferedReader br = new BufferedReader(fr);
			String line;
			String cmd;
			String from;
			String to;
			String multiTo[];
			String msgname;
			StringTokenizer st;
			List<Object> recvdmsgs;

			while ((line = br.readLine()) != null) {

				st = new StringTokenizer(line);
				cmd = st.nextToken();
				if (!cmd.startsWith("//")) { // '//' begins a comment line
					if (cmd.compareTo("send") == 0) {
						// message to send...<send> <from> <to[,to,to...]>
						// <name>
						from = st.nextToken();
						to = st.nextToken();

						if (to.indexOf(',') >= 0) { // multiple destinations

							StringTokenizer srt = new StringTokenizer(to, ",");
							multiTo = new String[srt.countTokens()];
							for (int numdests = 0; srt.hasMoreTokens();)
								multiTo[numdests++] = srt.nextToken();
							to = null;
						} else
							multiTo = null;

						msgname = st.nextToken();

						if (!processes.containsKey(from))
							processes.put(from, new CausallyOrderedMsgHandler(
									from));

						if (to != null) {
							if (!processes.containsKey(to))
								processes.put(to,
										new CausallyOrderedMsgHandler(to));
						} else
							for (int loop = 0; loop < multiTo.length; ++loop)
								if (!processes.containsKey(multiTo[loop]))
									processes.put(multiTo[loop],
											new CausallyOrderedMsgHandler(
													multiTo[loop]));

						if (multiTo != null) {
							for (int loop = 0; loop < multiTo.length; ++loop)
								if (messages.containsKey(multiTo[loop]
										.concat(msgname))) {
									System.out
											.println("Duplicate message botch");
									break;
								}

							CausallyOrderedMessage res[] = ((CausallyOrderedMsgHandler) processes
									.get(from)).sendMessage(msgname, multiTo);

							for (int loop = 0; loop < res.length; ++loop)
								messages.put(multiTo[loop].concat(msgname),
										res[loop]);
						} else {
							if (messages.containsKey(to.concat(msgname))) {
								System.out.println("Duplicate message botch");
								break;
							}

							messages.put(to.concat(msgname),
									((CausallyOrderedMsgHandler) processes
											.get(from))
											.sendMessage(msgname, to));
						}
					} else if (cmd.compareTo("recv") == 0) {
						// message to receive <recv> <at> <name>
						to = st.nextToken();
						msgname = st.nextToken();

						if (!processes.containsKey(to)) {
							System.out.println("Unknown receiver botch");
							break;
						}

						if (!messages.containsKey(to.concat(msgname))) {
							System.out.println("Unknown message botch");
							break;
						}

						recvdmsgs = (processes.get(to)).recvMessage(messages.get(to.concat(msgname)));

						if (recvdmsgs != null) {
							System.out.println("Process <" + to + ">"
									+ " received the following messages:");
							for (Iterator<Object> msgs = recvdmsgs.iterator(); msgs
									.hasNext();) {
								msgname = (String)msgs.next();
								System.out.println("\t" + msgname);
								messages.remove(to.concat(msgname));
							}
						}

					} else
						System.out.println("Syntax botch: <" + line + ">");

				}

			}
			br.close();
		} catch (java.util.NoSuchElementException e) {
		}

		catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}

		System.out.println("\n\nTime to complete tests: "
				+ (System.currentTimeMillis() - b4) + "\n\n");
		// check round-trip of time stamps through string

		for (Iterator<String> it = processes.keySet().iterator(); it.hasNext();) {
			String processName = it.next();
			String stringStamp = (processes.get(processName)).getClock()
					.toString();
			System.out.println(processName + " " + stringStamp);
			System.out.println(processName + " "
					+ new VectorTimeStamp(null, stringStamp).toString() + "\n");

		}
	}
}
