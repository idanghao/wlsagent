package com.beacon.wlsagent.jmxcube;

import java.io.IOException;
import java.rmi.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.apache.log4j.Logger;
import org.dom4j.Document;

import com.beacon.wlsagent.xml.XmlWorker;

public class BeaconStateCollector {

	// initialize necessary credentialss
	private String protocol;

	private String hostname;

	private String portString;

	private String username;

	private String password;

	private MBeanServerConnection connection;

	private JMXConnector connector;

	private static final ObjectName domainRuntimeServiceMBeanObjName;

	private Document coreDoc;

	private XmlWorker xmlw;

	private Map pinnedInfoMap;

	private ObjectName[] serverRT;

	private static Logger log;

	static {

		// Initializing the object name for DomainRuntimeServiceMBean
		// so it can be used throughout the class.
		try {
			log = Logger
					.getLogger(com.beacon.wlsagent.jmxcube.BeaconStateCollector.class);
			domainRuntimeServiceMBeanObjName = new ObjectName(
					"com.bea:Name=DomainRuntimeService,Type=weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean");
		} catch (MalformedObjectNameException e) {
			throw new AssertionError(e.getMessage());
		}
	}

	/**
	 */
	public BeaconStateCollector(Document doc) {
		super();
		xmlw = new XmlWorker();
		pinnedInfoMap = new HashMap(20);
		this.coreDoc = doc;
		String attrNameStrs[] = { "adminAddress", "adminPort", "principal",
				"password" };
		String[] coreFiledsStr = xmlw.getAttrValuesOfSingleNode(this.coreDoc,
				"//Domain", attrNameStrs);
		// this.hostname = coreFiledsStr[0];
		String adminAdd = System.getProperty("adminAdd");
		String adminProtocol = System.getProperty("adminProtocol");
		if (adminAdd == null || adminAdd.trim().equals(""))
			adminAdd = "127.0.0.1";
		if (adminProtocol == null)
			adminProtocol = "t3";
		else if (adminProtocol.equals("t3s"))
			adminProtocol = "t3s";
		this.hostname = adminAdd;
		this.protocol = adminProtocol;
		this.portString = coreFiledsStr[1];
		this.username = coreFiledsStr[2];
		this.password = coreFiledsStr[3];
	}

	public Document getCoreDoc() {
		return this.coreDoc;
	}

	/*
	 * Initialize connection to the Domain Runtime MBean Server
	 */
	public boolean initConnection() {
		try {
			Integer portInteger = Integer.valueOf(portString);
			int port = portInteger.intValue();
			String jndiroot = "/jndi/";
			String domainruntime = "weblogic.management.mbeanservers.domainruntime";
			JMXServiceURL serviceURL = new JMXServiceURL(protocol, hostname,
					port, jndiroot + domainruntime);
			Hashtable h = new Hashtable();
			h.put(Context.SECURITY_PRINCIPAL, username);
			h.put(Context.SECURITY_CREDENTIALS, password);
			h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES,
					"weblogic.management.remote");
			h.put("jmx.remote.x.request.waiting.timeout", new Long(10000));

			log.info("To connect to:" + serviceURL);
			
			connector = JMXConnectorFactory.connect(serviceURL, h);
			connection = connector.getMBeanServerConnection();
			serverRT = getServerRuntimes();
			log.debug("Got " + serverRT.length + " wls instances...\n");
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Cannot initialize connection due to: ", e);
			return false;
		}
	}

	/*
	 * Print an array of ServerRuntimeMBeans. This MBean is the root of the
	 * runtime MBean hierarchy, and each server in the domain hosts its own
	 * instance.
	 */
	private ObjectName[] getServerRuntimes() {
		try {
			return (ObjectName[]) connection.getAttribute(
					domainRuntimeServiceMBeanObjName, "ServerRuntimes");
		} catch (Exception e) {
			log.error("Cannot get ServerRuntimeMBeans: ", e);
			return null;
		}
	}

	public void refreshSvrList() {
		try {
			serverRT = getServerRuntimes();
			log.debug("Got " + serverRT.length + " wls instances...\n");
		} catch (Exception e) {
			log
					.error(
							"Connection to admin got error, Can not get server information, need to reconnect...",
							e);
			initConnection();
		}
		log.info("Server Name list refreshed...");
	}

	public ObjectName getServerRuntimesBySvrName(String sName) {
		try {
			int length = (int) serverRT.length;
			for (int i = 0; i < length; i++) {
				if (sName.equals((String) connection.getAttribute(serverRT[i],
						"Name"))) {
					return serverRT[i];
				}
			}
			return null;
		} catch (Exception e) {
			log.error("Cannot get ServerRuntimeMBean by given svrname: ", e);
			return null;
		}
	}

	public ObjectName[] getRuntimeMbeans(ObjectName parentMbeanObj,
			String sonMbeanObj) {
		try {
			if (connection != null) {
				if (sonMbeanObj.equals("ServerRuntime")) {
					ObjectName[] result = new ObjectName[1];
					result[0] = parentMbeanObj;
					return result;
				}
				String[] mbStruc = sonMbeanObj.split("\\.");
				ObjectName[] resultRtmMbeans;

				if (mbStruc.length <= 1) {
					if (connection.getAttribute(parentMbeanObj, sonMbeanObj) instanceof ObjectName) {
						resultRtmMbeans = new ObjectName[1];
						resultRtmMbeans[0] = (ObjectName) connection
								.getAttribute(parentMbeanObj, sonMbeanObj);
						return resultRtmMbeans;
					} else {
						resultRtmMbeans = (ObjectName[]) connection
								.getAttribute(parentMbeanObj, sonMbeanObj);
						return resultRtmMbeans;
					}
				} else {
					if (connection.getAttribute(parentMbeanObj, mbStruc[0]) instanceof ObjectName) {
						resultRtmMbeans = new ObjectName[1];
						resultRtmMbeans[0] = (ObjectName) connection
								.getAttribute(parentMbeanObj, mbStruc[0]);
					} else {
						resultRtmMbeans = (ObjectName[]) connection
								.getAttribute(parentMbeanObj, mbStruc[0]);
					}
				}

				List[] mbStrucArray = new List[mbStruc.length];

				mbStrucArray[0] = new ArrayList(resultRtmMbeans.length);
				for (int z = 0; z < resultRtmMbeans.length; z++) {
					mbStrucArray[0].add(resultRtmMbeans[z]);
				}

				int mbStrArrCounter = 0;
				for (int j = 1; j < mbStruc.length; j++) {

					if (mbStrucArray[mbStrArrCounter] == null) {
						mbStrucArray[mbStrArrCounter] = new ArrayList();
					}
					if (mbStrucArray[mbStrArrCounter + 1] == null) {
						mbStrucArray[mbStrArrCounter + 1] = new ArrayList();
					}
					int counter = mbStrucArray[mbStrArrCounter].size();
					for (int h = 0; h < counter; h++) {

						try {
							if (connection.getAttribute(
									(ObjectName) mbStrucArray[mbStrArrCounter]
											.get(h), mbStruc[j]) instanceof ObjectName) {
								mbStrucArray[mbStrArrCounter + 1]
										.add(connection
												.getAttribute(
														(ObjectName) mbStrucArray[mbStrArrCounter]
																.get(h),
														mbStruc[j]));
							} else {
								resultRtmMbeans = (ObjectName[]) connection
										.getAttribute(
												(ObjectName) mbStrucArray[mbStrArrCounter]
														.get(h), mbStruc[j]);

								if (mbStrucArray[mbStrArrCounter + 1] == null) {
									mbStrucArray[mbStrArrCounter + 1] = new ArrayList();
								}
								for (int x = 0; x < resultRtmMbeans.length; x++) {
									mbStrucArray[mbStrArrCounter + 1]
											.add(resultRtmMbeans[x]);
								}
							}

						} catch (AttributeNotFoundException ane) {
							log
									.debug(
											"cannot get wanted attribute from given mbean",
											ane);
							continue;
						}
					}
					mbStrArrCounter++;
				}

				int resultSize = mbStrucArray[mbStrArrCounter] == null ? 0
						: mbStrucArray[mbStrArrCounter].size();
				resultRtmMbeans = new ObjectName[resultSize];
				for (int v = 0; v < resultSize; v++) {
					resultRtmMbeans[v] = (ObjectName) mbStrucArray[mbStrArrCounter]
							.get(v);
				}
				return resultRtmMbeans;
			} else {
				return null;
			}
		} catch (ConnectException ce) {
			log
					.error(
							"Connection to admin got error, Can not get server information, need to reconnect...",
							ce);
			return null;
		} catch (Exception e) {
			log.error("Cannot get RuntimeMBean by " + parentMbeanObj + " and "
					+ sonMbeanObj, e);
			return null;
		}
	}

	/*
	 * Get Mbean object by name and parent MBean
	 */
	public ObjectName[] getRuntimeMbeans0ld(ObjectName parentMbeanObj,
			String sonMbeanObj) {
		try {
			if (connection != null) {
				if (sonMbeanObj.equals("ServerRuntime")) {
					ObjectName[] result = new ObjectName[1];
					result[0] = parentMbeanObj;
					return result;
				}
				String[] mbStruc = sonMbeanObj.split("\\.");
				List resultList = new ArrayList();
				ObjectName[] resultRtmMbeas = null;

				if (mbStruc.length <= 1) {
					if (connection.getAttribute(parentMbeanObj, sonMbeanObj) instanceof ObjectName) {
						resultRtmMbeas = new ObjectName[1];
						resultRtmMbeas[0] = (ObjectName) connection
								.getAttribute(parentMbeanObj, sonMbeanObj);
						return resultRtmMbeas;
					} else {
						resultRtmMbeas = (ObjectName[]) connection
								.getAttribute(parentMbeanObj, sonMbeanObj);
						return resultRtmMbeas;
					}
				} else {
					if (connection.getAttribute(parentMbeanObj, mbStruc[0]) instanceof ObjectName) {
						resultRtmMbeas = new ObjectName[1];
						resultRtmMbeas[0] = (ObjectName) connection
								.getAttribute(parentMbeanObj, mbStruc[0]);
					} else {
						resultRtmMbeas = (ObjectName[]) connection
								.getAttribute(parentMbeanObj, mbStruc[0]);
					}
				}
				for (int j = 1; j < mbStruc.length; j++) {
					int counter = resultRtmMbeas.length;
					for (int h = 0; h < counter; h++) {
						if (connection.getAttribute(resultRtmMbeas[h],
								mbStruc[j]) instanceof ObjectName) {
							resultRtmMbeas = new ObjectName[1];
							resultRtmMbeas[0] = (ObjectName) connection
									.getAttribute(resultRtmMbeas[h], mbStruc[j]);
						} else {
							resultRtmMbeas = (ObjectName[]) connection
									.getAttribute(resultRtmMbeas[h], mbStruc[j]);
						}
					}
				}
				return resultRtmMbeas;
			} else {
				return null;
			}
		} catch (Exception e) {
			log
					.error(
							"Cannot get RuntimeMBean by given parentMbeanObj and sonMbeanObj: ",
							e);
			return null;
		}
	}

	public Map[] getMBeanInfoMaps(String[] attrStrs, String mBeanType,
			ObjectName svrRt) {
		try {
			if (attrStrs != null) {

				ObjectName currRtmMbea[] = this.getRuntimeMbeans(svrRt,
						mBeanType);

				if (currRtmMbea != null) {
					Map[] mbeanInfoMap = new HashMap[currRtmMbea.length];

					for (int m = 0; m < currRtmMbea.length; m++) {
						List al;
						try {
							al = connection.getAttributes(currRtmMbea[m],
									attrStrs);
						} catch (InstanceNotFoundException ife) {
							log
									.debug(
											"cannot get wanted values from given mbean",
											ife);
							continue;
						}
						int attrindex = 0;
						mbeanInfoMap[m] = new HashMap(attrStrs.length);
						for (Iterator i = al.iterator(); i.hasNext(); attrindex++) {
							Attribute att = (Attribute) i.next();
							String curAttrVal = String.valueOf(att.getValue());
							if (curAttrVal == null) {
								mbeanInfoMap[m].put(attrStrs[attrindex], "n/a");
							} else {
								mbeanInfoMap[m].put(attrStrs[attrindex],
										curAttrVal);
							}
						}
					}

					return mbeanInfoMap;
				} else {
					return null;
				}
			} else {
				return null;
			}
		} catch (Exception e) {
			log
					.debug(
							"Cannot get MBeanInfoMap by given MBean and attributes Strings: ",
							e);
			return null;
		}
	}

	public void closeCollector() {
		try {
			connector.close();
		} catch (IOException ioe) {
			log.error("Cannot close connector: ", ioe);
		}
	}

	public Map genPinnedMap(boolean isInitial) {
		try {
			ObjectName domCfgMbean = this.getDomainConfigurationMbean();

			pinnedInfoMap.put("DomainVersion", connection.getAttribute(
					domCfgMbean, "DomainVersion"));
			pinnedInfoMap.put("AdminServerName", connection.getAttribute(
					domCfgMbean, "AdminServerName"));
			pinnedInfoMap.put("Name", connection.getAttribute(domCfgMbean,
					"Name"));
			pinnedInfoMap.put("ProductionModeEnabled", connection.getAttribute(
					domCfgMbean, "ProductionModeEnabled"));
			pinnedInfoMap.put("RootDirectory", connection.getAttribute(
					domCfgMbean, "RootDirectory"));

			ObjectName adminSvrRtmObjName = getServerRuntimesBySvrName((String) connection
					.getAttribute(domCfgMbean, "AdminServerName"));

			ObjectName jvmRtmObjName = (ObjectName) connection.getAttribute(
					adminSvrRtmObjName, "JVMRuntime");

			pinnedInfoMap.put("OSName", connection.getAttribute(jvmRtmObjName,
					"OSName"));
			pinnedInfoMap.put("OSVersion", connection.getAttribute(
					jvmRtmObjName, "OSVersion"));
			pinnedInfoMap.put("JavaVendor", connection.getAttribute(
					jvmRtmObjName, "JavaVendor"));
			pinnedInfoMap.put("JavaVersion", connection.getAttribute(
					jvmRtmObjName, "JavaVersion"));
			pinnedInfoMap.put("ActivationTime", connection.getAttribute(
					adminSvrRtmObjName, "ActivationTime"));

			pinnedInfoMap
					.put("ServerNum", String.valueOf(this.serverRT.length));

			return pinnedInfoMap;

		} catch (Exception e) {
			log.error("IsInitial: " + isInitial
					+ ". Cannot generate MBeanInfoMap...", e);
			return null;
		}
	}

	public ObjectName getDomainConfigurationMbean() {
		try {
			return (ObjectName) this.connection.getAttribute(
					this.domainRuntimeServiceMBeanObjName,
					"DomainConfiguration");
		} catch (Exception e) {
			log.error("Cannot get DomainConfigurationMbean: ", e);
			return null;
		}
	}

	public ObjectName[] getServerRT() {
		return this.serverRT;
	}

	public boolean isConnectedToAdmin() {
		try {
			this.connection.getDefaultDomain();
			return true;
		} catch (IOException ioe) {
			log.error("Connection to admin broken,need to reconnect...", ioe);
			initConnection();
			return false;
		}
	}

}