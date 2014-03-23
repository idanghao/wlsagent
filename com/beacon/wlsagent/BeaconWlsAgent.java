package com.beacon.wlsagent;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.io.*;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import com.beacon.util.BeaconUtil;
import com.beacon.wlsagent.jmxcube.BeaconStateCollector;
import com.beacon.wlsagent.qualification.Dog;
import com.beacon.wlsagent.xml.IniXmlValidator;
import com.beacon.wlsagent.xml.XmlWorker;

public class BeaconWlsAgent extends ServerSocket {

	private final static int BUFFER_LEN = 8192;

	private boolean isAlive;

	private Map coreMap;

	private static Logger log;

	private static Dog lic;

	static {
		log = Logger.getLogger(com.beacon.wlsagent.BeaconWlsAgent.class);
		lic = new Dog();
	}

	public BeaconWlsAgent(int port, boolean isValid) throws IOException {
		super(port);
		log.info("BeaconWlsAgent started at " + port + "...");
		if (isValid) {
			isAlive = true;
		}
		coreMap = new HashMap(6);
		coreMap.put("HANDINIT", "91");
		coreMap.put("HANDACK", "92");
		coreMap.put("GETSVRDATA", "93");
		coreMap.put("GETINITDATA", "94");
		coreMap.put("CLOSECONNECT", "95");
		coreMap.put("CLOSESVR", "96");
		coreMap.put("DUMP", "97");
		try {

			while (isAlive) {
				// Blocks until a connection occurs:
				Socket socket = accept();
				handleIncomingSocket(socket);
			}
		} catch (IOException ioe) {
			log.error("ServerSocket got errors during accept process...", ioe);
		} finally {
			close();
			log.info("BeaconWlsAgent closed...");
		}

	}

	public BeaconWlsAgent(int port, String adminPort, String usrnm, String pwd)
			throws IOException {
		super(port);
		log.info("BeaconWlsAgent started at " + port + "...");
		isAlive = true;
		coreMap = new HashMap(6);
		coreMap.put("HANDINIT", "91");
		coreMap.put("HANDACK", "92");
		coreMap.put("GETSVRDATA", "93");
		coreMap.put("GETINITDATA", "94");
		coreMap.put("CLOSECONNECT", "95");
		coreMap.put("CLOSESVR", "96");
		coreMap.put("DUMP", "97");
		try {

			while (isAlive) {
				// Blocks until a connection occurs:
				Socket socket = accept();
				handleIncomingSocket(socket);
			}
		} catch (IOException ioe) {
			log.error("ServerSocket got errors during accept process...", ioe);
		} finally {
			close();
			log.info("BeaconWlsAgent closed...");
		}

	}

	private void handleIncomingSocket(Socket socket) {
		try {
			PrintWriter pw = new PrintWriter(socket.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			char[] pubBuffer = new char[BUFFER_LEN];

			int len = br.read(pubBuffer, 0, 1);

			int iniRCode;

			if (len <= 0) {
				log.info("handleIncomingSocket: Cannot read any bytes from InputStream...");
			} else {
				iniRCode = (int) pubBuffer[0];
				if (iniRCode == Integer.valueOf(
						(String) coreMap.get("HANDINIT")).intValue()) {

					pw.write((char) Integer.parseInt((String) coreMap
							.get("HANDACK")));
					pw.flush();
					new WlsAgentThread(socket, br, pw);
					log.info("handleIncomingSocket: Handshake with client confirmed, forked a new wlsAgentThread for data collection...");
				} else if (iniRCode == Integer.valueOf((String) coreMap
						.get("CLOSESVR"))) {
					this.isAlive = false;
					pw.close();
					br.close();
					socket.close();
					log.info("handleIncomingSocket: Close request confirmed, wlsagent process will exit...");
				}
			}
		} catch (IOException ioe) {
			log.error(
					"handleIncomingSocket: ServerSocket got errors while dealing with incoming socket...",
					ioe);
		}
	}

	class WlsAgentThread extends Thread {
		private Socket clientSkt;

		private PrintWriter out;

		private BufferedReader in;

		private char[] buffer;

		private boolean thrdIsAlive;

		private BeaconStateCollector fsc;

		private XmlWorker xw;

		private String thrdMsg;

		boolean canConnect;

		public WlsAgentThread(Socket s, BufferedReader br, PrintWriter pw)
				throws IOException {
			super("Beacon.WlsAgentThread");
			clientSkt = s;
			thrdIsAlive = true;
			out = pw;
			in = br;
			xw = new XmlWorker(lic);
			thrdMsg = new String("WlsAgentThread OK!");
			canConnect = false;
			start(); // Calls run()
		}

		private Document genInputDoc() {
			try {
				// get length of incoming-xml
				int len = in.read(buffer, 0, 8);

				// get incoming xml
				String lenStr = new String(buffer, 0, len);
				int inXmlLen = Integer.parseInt(lenStr.trim());
				log.debug("ready to read " + inXmlLen
						+ " bytes to generate incoming-xml:");
				int readInLen = in.read(buffer, 0, inXmlLen);

				StringBuffer sb = new StringBuffer();
				String inXml = new String(buffer, 0, readInLen);
				sb.append(inXml);
				// judge the real length per read, if truncated by network
				// situation, we need to keep reading until get whole
				// content
				if (readInLen < inXmlLen) {
					int dif = inXmlLen - readInLen;
					readInLen = in.read(buffer, 0, dif);
					sb.append(new String(buffer, 0, dif));
					readInLen = readInLen + dif;
				}
				// String xmlAddress = "D:\\ini.xml";
				// SAXReader reader = new SAXReader();
				// File xmlFile = new File(xmlAddress);
				// Document document = reader.read(xmlFile);
				// inXml = document.asXML();
				log.debug("incoming initial xml: " + inXml);
				return BeaconUtil.string2Document(sb.toString());
			} catch (Exception ioe) {
				log.error(
						"genInputDoc: got IOException while generatinging input document...",
						ioe);
				this.thrdIsAlive = false;
				return null;
			}
		}

		private void feedData(String reason) {
			try {
				Document inputDoc;
				if (reason != null) {

					if (reason.equals("GETINITDATA")) {
						// XML validation at pre-defined schema
						IniXmlValidator ixv = new IniXmlValidator();
						inputDoc = genInputDoc();

						if (ixv.validateXMLByXSD(inputDoc)) {
							fsc = new BeaconStateCollector(inputDoc);
							canConnect = fsc.initConnection();
							if (!canConnect) {
								this.thrdMsg = "Can't attach to WebLogic Admin. maybe shutdown.";
							}
						}

						if (fsc == null) {
							fsc = new BeaconStateCollector(inputDoc);
						}
						String iniStr = xw.genFeedData(true, this.fsc).asXML();
						buffer = BeaconUtil.bytesToChars(iniStr.getBytes());
						log.debug("Pinned info:  " + iniStr);
					}

					if (reason.equals("DUMP")) {
						inputDoc = genInputDoc();
						// TODO Here to add the logic about getting thread dump
						// info by using JVMRuntimeBean
						buffer = BeaconUtil.bytesToChars(xw
								.genErrXml("Thread Dump: ----- ").asXML()
								.getBytes());
					}

				} else {
					String feedStr = xw.genFeedData(false, this.fsc).asXML();
					buffer = BeaconUtil.bytesToChars(feedStr.getBytes());
					log.debug(feedStr);
				}

				if (buffer == null || buffer.length == 0 || canConnect == false) {
					buffer = BeaconUtil.bytesToChars(xw.genErrXml(thrdMsg)
							.asXML().getBytes());
				}

				String sLen = String.valueOf(buffer.length);

				if (sLen.length() < 9) {
					int dif = 9 - sLen.length();
					StringBuffer zeroStr = new StringBuffer("");
					for (int m = 0; m < dif; m++) {
						zeroStr = zeroStr.append("0");
					}
					sLen = zeroStr.toString() + sLen;
				}

				char[] charLen = sLen.toCharArray();
				for (int i = 0; i < charLen.length; i++) {
					out.write(charLen[i]);
				}

				out.flush();
				log.debug("write feed-data's length into OutputStream..."
						+ String.valueOf(charLen));
				out.write(buffer);
				out.flush();
				log.debug("write feed-data into OutputStream...");
			} catch (Exception e) {
				log.error(
						"feedData: got RuntimeException while feeding data back to client...",
						e);
				this.thrdIsAlive = false;
			}
		}

		private void feedData() {
			feedData(null);
		}

		public void run() {
			try {
				log.info("A new WlsAgentThread started...");
				buffer = new char[BUFFER_LEN];

				while (thrdIsAlive && this.clientSkt.isConnected()) {

					int len = in.read(buffer, 0, 1);

					int rCode;

					if (len <= 0) {
						log.info("Cannot read any bytes from InputStream...");
						this.sleep(5000);
						break;
					} else {

						if (this.fsc != null)
							fsc.refreshSvrList();

						rCode = (int) buffer[0];

						log.debug("HANDSHAKE CODE IS: " + String.valueOf(rCode));

						switch (rCode) {

						case 93:
							log.debug("GETSVRDATA");
							feedData();
							break;

						case 94:
							log.debug("GETINITDATA");
							feedData("GETINITDATA");
							break;

						case 95:
							log.debug("CLOSECONNECT");
							thrdIsAlive = false;
							in.close();
							out.close();
							clientSkt.close();
							break;
						default:
							log.debug("NO PROPER CODE");
							thrdIsAlive = false;
							in.close();
							out.close();
							clientSkt.close();
							break;

						}
					}

					log.info("a collection loop done...");
					try {
						lic.silentPresume();
					} catch (Exception ex) {
						thrdIsAlive = false;
					}
				}
				this.fsc.closeCollector();
				log.info("Collection done , current WlsAgentThread will exit...");
			} catch (IOException ioe) {
				log.error(
						"Errors occured during WlsAgentThread run-circle ...",
						ioe);
				thrdIsAlive = false;
			} catch (InterruptedException ie) {
				log.error("Errors occured while WlsAgentThread sleeping  ...",
						ie);
				thrdIsAlive = false;
			} finally {
				try {
					in.close();
					out.close();
					clientSkt.close();
					log.info("Client socket closed...");
				} catch (IOException ioe) {
					log.error(
							"Errors occured when try to close clent socket and its' streams ...",
							ioe);
				}
			}
		}

	}

	public static void start(String[] args) {
		try {
			BeaconUtil.initLog4j("resource/log4j.properties");

			boolean isValid = lic.presume();
			if (args.length < 1) {
				log.info("usage:  BeaconWlsAgent portNum.\nNow using 8108 as default listen port...");
				new BeaconWlsAgent(8108, isValid);
			} else {
				new BeaconWlsAgent(Integer.parseInt(args[0]), isValid);
			}

		} catch (IOException ioe) {
			log.error("BeaconWlsAgent failed to start for following reasons: ",
					ioe);
		} catch (BeaconException fse) {
			log.error(fse.getMessage());
		}

	}
}
