package com.beacon.wlsagent.xml;

import java.io.File;

import java.util.ArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;

import org.apache.log4j.Logger;

import org.dom4j.Document;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import org.dom4j.io.SAXReader;

import com.beacon.util.BeaconUtil;
import com.beacon.wlsagent.jmxcube.BeaconStateCollector;
import com.beacon.wlsagent.qualification.Dog;

public class XmlWorker {

	private static Logger log;

	private static Dog lic;

	static {
		log = Logger.getLogger(com.beacon.wlsagent.xml.XmlWorker.class);
	}

	/**
	 */
	public XmlWorker(Dog lic) {
		super();
		this.lic = lic;
	}

	/**
	 */
	public XmlWorker() {
		super();
	}

	public Document genIniXml(Map pinnedInfoMap) {

		Document document = DocumentHelper.createDocument();

		Element root = document.addElement("MONITOR").addAttribute("Date",
				BeaconUtil.getStringDate());
		if (pinnedInfoMap != null) {

			String agentVer = lic.getIp().equals("ANY") ? "- TRIAL LICENSE"
					: "- FORMAL LICENSE";
			agentVer = "1.0 " + agentVer;

			root.addElement("INITBUF")
					.addAttribute("AdminServerName",
							(String) pinnedInfoMap.get("AdminServerName"))
					.addAttribute("DomainVersion",
							(String) pinnedInfoMap.get("DomainVersion"))
					.addAttribute("Name", (String) pinnedInfoMap.get("Name"))
					.addAttribute(
							"ProductionModeEnabled",
							((Boolean) pinnedInfoMap
									.get("ProductionModeEnabled")).toString())
					.addAttribute("RootDirectory",
							(String) pinnedInfoMap.get("RootDirectory"))
					.addAttribute(
							"Activationtime",
							BeaconUtil.longToDateStr((Long) pinnedInfoMap
									.get("ActivationTime")))
					.addAttribute("JDKVendor",
							(String) pinnedInfoMap.get("JavaVendor"))
					.addAttribute("JDKVersion",
							(String) pinnedInfoMap.get("JavaVersion"))
					.addAttribute("OSVersion",
							(String) pinnedInfoMap.get("OSVersion"))
					.addAttribute("OSName",
							(String) pinnedInfoMap.get("OSName"))
					.addAttribute("ServerNum",
							(String) pinnedInfoMap.get("ServerNum"))
					.addAttribute("AgentVersion", "1.0")
					.addAttribute("LICTYPE", lic.getIp());
			;
		}
		return document;
	}

	public Document genErrXml(String sysCode) {

		Document document = DocumentHelper.createDocument();

		Element root = document.addElement("MONITOR").addAttribute("Date",
				BeaconUtil.getStringDate());

		root.addElement("SYSTEM").addAttribute("errMsg", sysCode);
		return document;
	}

	public Document genMonXml(Document document, BeaconStateCollector fsc) {
		Document resultDoc = null;

		if (fsc.isConnectedToAdmin()) {

			resultDoc = DocumentHelper.createDocument();

			Element root = resultDoc.addElement("MONITOR").addAttribute("Date",
					BeaconUtil.getStringDate());
			Element osEle = root.addElement("OSResource");

			String cpuStr = BeaconUtil.runShell("sh bin/getCPU.sh");
			String memStr = BeaconUtil.runShell("sh bin/getMEM.sh");
			if ((cpuStr == "") || (memStr == "")) {
				cpuStr = (String) BeaconUtil.runShell().get("CPU");
				memStr = (String) BeaconUtil.runShell().get("MEM");
			}
			osEle.addAttribute("CPU", cpuStr).addAttribute("MEM", memStr);

			List mbeanEleList = document.selectNodes("//MBean");

			ObjectName[] svrRt = fsc.getServerRT();

			int svrRtCount = svrRt.length;

			int f = 0;
			for (Iterator i = mbeanEleList.iterator(); i.hasNext();) {
				Element mbeanEle = (Element) i.next();
				String mbeanName = mbeanEle.attributeValue("name");
				log.debug("Currently dealing with mbean: " + mbeanName);
				Iterator k = mbeanEle.elementIterator("attribute");

				List al = new ArrayList();
				for (; k.hasNext(); f++) {
					al.add(((Element) k.next()).getText());
				}
				String mbeanAttrStrs[] = BeaconUtil.listToStrArray(al);

				for (int m = 0; m < svrRtCount; m++) {
					Map mBeanInfoMap[] = fsc.getMBeanInfoMaps(mbeanAttrStrs,
							mbeanName, svrRt[m]);
					String svrName = svrRt[m].getKeyProperty("Name");
					log.debug("Currently dealing with server: " + svrRt[m]);
					if (mBeanInfoMap != null) {
						for (int g = 0; g < mBeanInfoMap.length; g++) {
							if (mbeanAttrStrs.length > 0) {
								Element currServer = root.addElement(mbeanName);
								for (int j = 0; j < mbeanAttrStrs.length; j++) {
									String curAttr = mbeanAttrStrs[j];
									if (mBeanInfoMap[g] != null) {
										currServer.addAttribute(curAttr, String
												.valueOf(mBeanInfoMap[g]
														.get(curAttr)));
										log.debug("Attribute: "
												+ curAttr
												+ " Value: "
												+ String.valueOf(mBeanInfoMap[g]
														.get(curAttr)));
									}
								}
								currServer.addAttribute("serverName", svrName);
							}
						}
					}
				}

			}
		} else {
			resultDoc = this.genErrXml("Happened to lose connection to WebLogic Admin. maybe shutdown");
		}

		return resultDoc;
	}

	public String[] getAttrValuesOfSingleNode(Document document, String xpath,
			String[] attrNameStrs) {

		Element domEle = (Element) document.selectSingleNode(xpath);

		log.info(document);
		int attrCount = attrNameStrs.length;
		String attrValues[] = new String[attrCount];
		for (int i = 0; i < attrCount; i++) {
			attrValues[i] = domEle.attribute(attrNameStrs[i]).getText();
		}
		return attrValues;

	}

	public Document genFeedData(boolean isInitial, BeaconStateCollector fsc) {
		try {
			if (isInitial) {
				return this.genIniXml(fsc.genPinnedMap(isInitial));

			} else {
				return this.genMonXml(fsc.getCoreDoc(), fsc);
			}
		} catch (Exception e) {
			log.error("IsInitial: " + isInitial
					+ ". Cannot generate MBeanInfoMap...", e);
			return null;
		}
	}
}
