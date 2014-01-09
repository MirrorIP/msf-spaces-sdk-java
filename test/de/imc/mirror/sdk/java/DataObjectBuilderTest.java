package de.imc.mirror.sdk.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.java.cdm.CDMData_1_0;
import de.imc.mirror.sdk.java.cdm.CDMData_2_0;
import de.imc.mirror.sdk.java.exceptions.InvalidBuildException;

public class DataObjectBuilderTest {
	String exampleXMLString;
	Element exampleXML;
	
	@Before
	public void initializeTests() {
		exampleXML = new Element("ping", "mirror:application:ping:ping");
		exampleXML.setAttribute("cdmVersion", "1.0");
		exampleXML.setAttribute("id", "n/a");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(10000l));
		exampleXML.setAttribute("timestamp", DatatypeConverter.printDateTime(calendar));
		exampleXML.setAttribute("modelVersion", "1.0");
		Element contentElement = new Element("content", exampleXML.getNamespace());
		contentElement.setText("my content");
		exampleXML.addContent(contentElement);
		XMLOutputter outputter = new XMLOutputter();
		exampleXMLString = outputter.outputString(exampleXML);
	}
	
	@Test
	public void testPingCreation() {
		CDMDataBuilder cdmBuilder = new CDMDataBuilder(CDMVersion.CDM_1_0);
		cdmBuilder.setTimestamp(exampleXML.getAttributeValue("timestamp"));
		cdmBuilder.setModelVersion("1.0");
		DataObjectBuilder builder = new DataObjectBuilder("ping", "mirror:application:ping:ping");
		try {
			builder.setCDMData(cdmBuilder.build());
		} catch (InvalidBuildException e) {
			fail("Failed to apply CDM data: " + e.getMessage());
			return;
		}
		builder.addElement("content", "my content", false);
		DataObject dataObject = builder.build();
		String dataObjectString = dataObject.toString();
		assertEquals(exampleXMLString, dataObjectString);
	}
	
	@Test
	public void testElementConstructor() {
		Element rootElement = exampleXML.clone();
		DataObjectBuilder builder = new DataObjectBuilder(rootElement, rootElement.getNamespaceURI());
		DataObject dataObject = builder.build();
		String dataObjectString = dataObject.toString();
		assertEquals(exampleXMLString, dataObjectString);
	}
	
	@Test
	public void testAttributeHandling() {
		Element compareElement = exampleXML.clone();
		compareElement.setAttribute("attr1", "val1");
		compareElement.setAttribute("attr2", "val2");
		compareElement.setAttribute("attr3", "val3");
		String compareString = new XMLOutputter().outputString(compareElement);

		Element rootElement = exampleXML.clone();
		DataObjectBuilder builder = new DataObjectBuilder(rootElement, rootElement.getNamespaceURI());
		builder.setAttribute("attr1", "val1");
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("attr2", "val2");
		attributes.put("attr3", "val3");
		builder.setAttributes(attributes);
		DataObject dataObject = builder.build();
		String dataObjectString = dataObject.toString();
		assertEquals(compareString, dataObjectString);
	}
	
	@Test
	public void testElementHandling() {
		Element compareElement = exampleXML.clone();
		Element e1 = new Element("stringElem", compareElement.getNamespace());
		e1.setText("foo");
		compareElement.addContent(e1);
		Element e2 = new Element("parsedElem", compareElement.getNamespace());
		e2.addContent(new Element("foo", compareElement.getNamespace()));
		compareElement.addContent(e2);
		Element e3 = new Element("generatedElem", compareElement.getNamespace());
		e3.setAttribute("foo", "bar");
		compareElement.addContent(e3);
		Element e4 = new Element("xmlElem", compareElement.getNamespace());
		e4.setAttribute("foo", "bar");
		compareElement.addContent(e4);
		String compareString = new XMLOutputter().outputString(compareElement);
		
		Element rootElement = exampleXML.clone();
		DataObjectBuilder builder = new DataObjectBuilder(rootElement, rootElement.getNamespaceURI());
		builder.addElement("stringElem", "foo", false);
		builder.addElement("parsedElem", "<foo />", true);
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("foo", "bar");
		builder.addElement("generatedElem", attributes, null, false);
		Element xmlElem = new Element("xmlElem", rootElement.getNamespace());
		xmlElem.setAttribute("foo", "bar");
		builder.addElement(xmlElem);
		
		DataObject dataObject = builder.build();
		String dataObjectString = dataObject.toString();
		assertEquals(compareString, dataObjectString);
	}
	
	@Test
	public void testReadDataObjectWithCDMVersionAttribute_1_0() {
		Element rootElement = exampleXML.clone();
		DataObjectBuilder builder = new DataObjectBuilder(rootElement, rootElement.getNamespaceURI());
		assertEquals("Incorrect CDM version.", CDMVersion.CDM_1_0, ((CDMData) builder.build().getCDMData()).getCDMVersion());
		assertTrue("Incorrect CDM instance.", builder.build().getCDMData() instanceof CDMData_1_0);
	}

	@Test
	public void testReadDataObjectWithCDMVersionAttribute_2_0() {
		Element rootElement = exampleXML.clone();
		rootElement.setAttribute("cdmVersion", "2.0");
		DataObjectBuilder builder = new DataObjectBuilder(rootElement, rootElement.getNamespaceURI());
		assertEquals("Incorrect CDM version.", CDMVersion.CDM_2_0, ((CDMData) builder.build().getCDMData()).getCDMVersion());
		assertTrue("Incorrect CDM instance.", builder.build().getCDMData() instanceof CDMData_2_0);
	}

	@Test
	public void testReadDataObjectWithoutCDMVersionAttribute() {
		Element rootElement = exampleXML.clone();
		rootElement.removeAttribute("cdmVersion");
		DataObjectBuilder builder = new DataObjectBuilder(rootElement, rootElement.getNamespaceURI());
		DataObject dataObject = builder.build();
		
		CDMData cdmData = (CDMData) dataObject.getCDMData();
		assertEquals("Incorrect CDM version.", CDMVersion.CDM_2_0, cdmData.getCDMVersion());
		assertTrue("Incorrect CDM instance.", cdmData instanceof CDMData_2_0);
	}
}

