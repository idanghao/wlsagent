/**
 * 
 */
package com.beacon.wlsagent.jmxcube;

import org.dom4j.Document;

/**
 * @author hdang
 * 
 */
public interface IBeaconStateCollector {

	/*
	 * Initialize connection to the Domain Runtime
	 */
	public boolean initConnection();
	
	/*
	 * close connection to the Domain Runtime in case of close failure, just
	 * throw Exception upwards
	 */
	public void closeCollector();

	/*
	 * refresh the member list of domain, timely called
	 */
	public void refreshSvrList();


	/*
	 * be responsible to generate feed data, divided into two conditions:
	 * 1)isInitial=true , then generate initial feeding data , refer to ini.xml
	 * 2)isInitial=false, then generate general monitoring data(runtime) , refer
	 * to mon_result.xml
	 */
	public Document genFeedData(boolean isInitial);

	/*
	 * provide error prompt in xml format by sysCode parameter
	 */
	public Document genErrXml(String sysCode);

	/*
	 * extract specific attributes from given xml 
	 * e.g.  document=iniParameter.xml
	 * 		 String attrNameStrs[] = { "adminAddress", "adminPort", "principal",
				"password" };
		     String[] coreFiledsStr = xmlw.getAttrValuesOfSingleNode(document,
				"//Domain", attrNameStrs);
	 */
	public String[] getAttrValuesOfSingleNode(Document document, String xpath,
			String[] attrNameStrs);
}
