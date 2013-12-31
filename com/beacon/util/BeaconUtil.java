package com.beacon.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;

public class BeaconUtil {

	private static Logger log;

	static {
		log = Logger.getLogger(com.beacon.util.BeaconUtil.class);
	}

	// convert string to Document

	public static Document string2Document(String s) {
		Document doc = null;
		try {
			doc = DocumentHelper.parseText(s);
		} catch (Exception ex) {
			log
					.error(
							"Errors occured while converting string to org.dom4j.Document",
							ex);
		}
		return doc;
	}

	public static String bytesToString(byte[] bufferBytes) {
		int len = 0;
		if (bufferBytes != null) {
			len = bufferBytes.length;
		}
		return new String(bufferBytes, 0, len);
	}

	public static char[] bytesToChars(byte[] bytes) {
		String s = new String(bytes);
		char chars[] = s.toCharArray();
		return chars;
	}

	// Get current date (return yyyy-MM-dd HH:mm:ss)

	public static String getStringDate() {
		Date currentTime = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = formatter.format(currentTime);
		return dateString;
	}

	public static long showAsMeg(Long origNum) {
		return ((origNum.longValue() / 1024) / 1024);
	}

	public static void initLog4j(String log4jurl) {
		java.net.URL configFileUrl = ClassLoader.getSystemResource(log4jurl);
		PropertyConfigurator.configure(configFileUrl);
	}

	public static String longToDateStr(Long activationTime) {
		Calendar cal = Calendar.getInstance();
		Date date = cal.getTime();
		date.setTime(activationTime);
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String strDateTime = formater.format(date);
		return strDateTime;
	}

	
	public static String genDateStrByFormat(Date date) {
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String strDateTime = formater.format(date);
		return strDateTime;
	}	
	
	
	public static String[] listToStrArray(List list) {
		if (list != null) {
			String curStr[] = new String[list.size()];
			int n = 0;
			for (Iterator j = list.iterator(); j.hasNext(); n++) {
				curStr[n] = (String) j.next();
			}
			return curStr;
		}
		return null;
	}
	public static void dumpThread(String url, String username, String password,
			int dumpCount) {
		String[] args = new String[7];
		String msg;
		// -url t3://localhost:7001 -username weblogic -password weblogic

		args[0] = "-url";
		args[1] = "t3://" + url;
		args[2] = "-username";
		args[3] = username;
		args[4] = "-password";
		args[5] = password;
		args[6] = "THREAD_DUMP";

		try {
			for (int i = 0; i < dumpCount; i++) {
				//TODO here ***************************				
				msg = "No." + i + " dump thread for server: " + url;
				log.info(msg);
				Thread.currentThread().sleep(7000);
			}
		} catch (Exception ex) {
			msg = "Errors occured while dumping threads from current JVM";
			log.error(msg, ex);
		}

	}
	public static String runShell(String paramString) {
		{
			InputStream fis;
			InputStreamReader isr;
			BufferedReader br;
			String str1 = "";
			String str2;
			try {
				Process proc = Runtime.getRuntime().exec(paramString);
				if (proc == null) {
					log.error("Cannot get process while executing shell...");
					return null;
				}
				fis = proc.getInputStream();
				isr = new InputStreamReader(fis);
				br = new BufferedReader(isr);
			} catch (IOException localIOException1) {
				log.error("Got IOException while executing shell...");
				return null;
			}

			try {
				while ((str2 = br.readLine()) != null) {
					str1 = str1 + str2;
					str1 = str1 + "\n";
				}
				log.info("stdout:\n" + str1);
				return str1;
			} catch (IOException localIOException2) {
				System.out
						.println("Got IOException while reading shell result...");
				try {
					while ((str2 = br.readLine()) != null) {
						str1 = str1 + str2;
						str1 = str1 + "\n";
					}
					log.error("stderr:\n" + str1);
					return str1;
				} catch (IOException localIOException3) {
					System.out
							.println("Got IOException while reading shell result...");
					return null;
				}
			}
		}

	}

	public static Map runShell() {
		HashMap localHashMap = new HashMap(2);
		InputStream fis;
		InputStreamReader isr;
		BufferedReader br;
		String str1 = "";
		String str2;
		try {
			Process proc = Runtime.getRuntime().exec("cscript bin\\winos.vbs");
			if (proc == null) {
				log.error("Cannot get process while executing shell...");
				return null;
			}
			fis = proc.getInputStream();
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
		} catch (IOException localIOException1) {
			log.error("Got IOException while executing shell...");
			return null;
		}
		try {
			String line = null;
			for (int i = 0; (line = br.readLine()) != null; ++i) {
				if (i == 3)
					localHashMap.put("MEM", line);
				if (i != 4)
					continue;
				localHashMap.put("CPU", line);
			}
			//System.out.println("stdout:\n" + localHashMap.toString());
			return localHashMap;

		} catch (IOException localIOException2) {
			log.error("Got IOException while reading shell result...");
			try {
				while ((str2 = br.readLine()) != null) {
					str1 = str1 + str2;
					str1 = str1 + "\n";
				}
				log.error("stderr:\n" + str1);
				return null;
			} catch (IOException localIOException3) {
				System.out
						.println("Got IOException while reading shell result...");
				return null;
			}
		}
	}	
	
	
}
