package de.schlichtherle.util;

import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetAddresses {

	static boolean verbose = false;

	private boolean success;

	public NetAddresses() {
		success = false;
	}

	public static boolean isValid(String ip) {
		if (ip != null && ip.equals("ANY")) {
			return true;
		}

		try {
			InetAddress ainetaddress[] = null;
			InetAddress inetaddress = null;
			try {
				inetaddress = InetAddress.getLocalHost();
			} catch (UnknownHostException unknownhostexception) {
				System.out
						.println("Couldn't even run InetAddress.getLocalHost(): "
								+ unknownhostexception
								+ "\n"
								+ "There's no good way to work around that.");
				return false;
			}
			try {
				String s = inetaddress.getHostName();
				say("Got host name: " + s);
				ainetaddress = InetAddress.getAllByName(s);
				say("Found " + ainetaddress.length + " addresse(s) for " + s);
			} catch (UnknownHostException unknownhostexception1) {
				say("Couldn't determine host addresse(s) by name, trying by address.");
				try {
					String s1 = inetaddress.getHostAddress();
					say("Got host addr: " + s1);
					ainetaddress = InetAddress.getAllByName(s1);
					say("Found " + ainetaddress.length + " addresse(s) for "
							+ s1);
				} catch (UnknownHostException unknownhostexception2) {
					System.out.println("Couldn't determine host addresse(s)");
					return false;
				}
			}
			for (int i = 0; i < ainetaddress.length; i++) {
				String curIp = ainetaddress[i].getHostAddress();
				if (curIp.equals(ip)) {
					return true;
				}
			}
			return false;

		} catch (Exception exception) {
			internalError(exception);
			return false;
		}
	}

	static void say(String s) {
		if (verbose)
			System.out.println("* " + s);
	}

	static void internalError(Throwable throwable) {
		System.out
				.println("Internal error:\nPlease mail the following stack trace to <fusionspystudion@gmail.com>\n");
		throwable.printStackTrace(System.out);
	}

}
