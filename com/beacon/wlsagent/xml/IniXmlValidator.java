package com.beacon.wlsagent.xml;

import java.io.File;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.SAXValidator;
import org.dom4j.io.XMLWriter;
import org.dom4j.util.XMLErrorHandler;

import com.beacon.util.BeaconUtil;

public class IniXmlValidator {
	// Create default Error handler
	private XMLErrorHandler errorHandler;

	private String xsdFileName;

	private SAXParserFactory factory;

	private SAXParser parser;

	private SAXValidator validator;

	private XMLWriter writer;

	private static Logger log;

	static {
		log = Logger.getLogger(com.beacon.wlsagent.xml.IniXmlValidator.class);
	}

	public IniXmlValidator() {
		try {
			String curDir = System.getProperty("user.dir");
			String fileBar = System.getProperty("file.separator");
			xsdFileName = curDir + fileBar + fileBar + "resource"
					+ fileBar + "ini.xsd";
			errorHandler = new XMLErrorHandler();

			factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			factory.setNamespaceAware(true);

			parser = factory.newSAXParser();

			parser.setProperty(
					"http://java.sun.com/xml/jaxp/properties/schemaLanguage",
					"http://www.w3.org/2001/XMLSchema");
			parser.setProperty(
					"http://java.sun.com/xml/jaxp/properties/schemaSource",
					"file:" + xsdFileName);

			validator = new SAXValidator(parser.getXMLReader());

			validator.setErrorHandler(errorHandler);

			writer = new XMLWriter(OutputFormat.createPrettyPrint());
		} catch (Exception e) {
			log.error("cannot initialize Xml validator: ", e);
		}
	}

	/**
	 * initial xml file validation
	 */
	public boolean validateXMLByXSD(Document xmlDocument) {

		try {
			// validation
			validator.validate(xmlDocument);

			// print error info if any occurs
			if (errorHandler.getErrors().hasContent()) {
				log.info("initial xml validation failed!\n");
				writer.write(errorHandler.getErrors());
				return false;
			} else {
				log.info(("initial xml validation succeed!!"));
				return true;
			}
		} catch (Exception e) {
			log.error("Validation failed: ", e);
			return false;
		}
	}

	public static void validateXML(String[] args) {
		try {
			IniXmlValidator ixv = new IniXmlValidator();

			// 创建一个读取工具
			SAXReader xmlReader = new SAXReader();
			// 获取要校验xml文档实例
			Document xmlDocument = (Document) xmlReader.read(new File(args[0]));
			ixv.validateXMLByXSD(xmlDocument);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
