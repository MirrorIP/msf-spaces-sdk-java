package de.imc.mirror.sdk.java;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import de.imc.mirror.sdk.cdm.CDMData_1_0;
import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.cdm.Reference.ReferenceType;
import de.imc.mirror.sdk.java.cdm.CDMData_2_0;
import de.imc.mirror.sdk.java.cdm.CreationInfo;
import de.imc.mirror.sdk.java.cdm.Reference;
import de.imc.mirror.sdk.java.cdm.References;
import de.imc.mirror.sdk.java.cdm.Summary;
import de.imc.mirror.sdk.java.exceptions.InvalidBuildException;

public class CDMDataBuilderTest {
	private Element testElement;
	private Date testDate;
	private XMLOutputter xmlOutputter; 
	
	@Before
	public void initializeTest() {
		testElement = new Element("testElement", "test:namespace");
		testDate = new Date(335179977000l);
		xmlOutputter = new XMLOutputter();
	}
	
	@Test
	public void testMinialCDM_1_0_Build() {
		CDMDataBuilder builder = new CDMDataBuilder(CDMVersion.CDM_1_0);
		builder.setModelVersion("1.1");
		CDMData cdmData;
		try {
			cdmData = builder.build();
		} catch (InvalidBuildException e) {
			fail("Build of minimal CDM 1.0 failed: " + e.getMessage());
			return;
		}
		assertEquals("Invalid CDM version was set internally.", cdmData.getCDMVersion(), CDMVersion.CDM_1_0);
		assertTrue("Invalid CDM class instance.", cdmData instanceof CDMData_1_0);
		CDMData_1_0 typedCdmData = (CDMData_1_0) cdmData;
		assertEquals("Invalid model version.", "1.1", typedCdmData.getModelVersion());
		
		cdmData.applyToElement(testElement);
		assertEquals("Missing or wrong modelVersion attribute.", "1.1", testElement.getAttributeValue("modelVersion"));
	}

	@Test
	public void testFullCDM_1_0_Build() {
		CDMDataBuilder builder = new CDMDataBuilder(CDMVersion.CDM_1_0);
		builder.setModelVersion("1.1");
		builder.setCustomId("12345");
		builder.setPublisher("me");
		builder.setRef("8eafa5e6-be54-43b2-8152-ab072f54ee3f");
		
		CDMData cdmData;
		try {
			cdmData = builder.build();
		} catch (InvalidBuildException e) {
			fail("Build of full CDM 1.0 failed: " + e.getMessage());
			return;
		}
		assertEquals("Invalid CDM version was set internally.", cdmData.getCDMVersion(), CDMVersion.CDM_1_0);
		assertTrue("Invalid CDM instance.", cdmData instanceof CDMData_1_0);
		CDMData_1_0 typedCdmData = (CDMData_1_0) cdmData;
		assertEquals("Invalid model version.", "1.1", typedCdmData.getModelVersion());
		assertEquals("Invalid custom id.", "12345", typedCdmData.getCustomId());
		assertEquals("Invalid publisher.", "me", typedCdmData.getPublisher());
		assertEquals("Invalid ref.", "8eafa5e6-be54-43b2-8152-ab072f54ee3f", typedCdmData.getRef());
		
		cdmData.applyToElement(testElement);

		assertEquals("Missing or wrong cdmVersion attribute.", "1.0", testElement.getAttributeValue("cdmVersion"));
		assertEquals("Missing or wrong modelVersion attribute.", "1.1", testElement.getAttributeValue("modelVersion"));
		assertEquals("Missing or wrong customId attribute.", "12345", testElement.getAttributeValue("customId"));
		assertEquals("Missing or wrong publisher attribute.", "me", testElement.getAttributeValue("publisher"));
		assertEquals("Missing or wrong ref attribute.", "8eafa5e6-be54-43b2-8152-ab072f54ee3f", testElement.getAttributeValue("ref"));
	}
	
	@Test
	public void testMinimalCDM_2_0_Build() {
		CDMDataBuilder builder = new CDMDataBuilder(CDMVersion.CDM_2_0);
		builder.setModelVersion("1.1");
		CDMData cdmData;
		try {
			cdmData = builder.build();
		} catch (InvalidBuildException e) {
			fail("Build of minimal CDM 2.0 failed: " + e.getMessage());
			return;
		}
		assertEquals("Invalid CDM version was set internally.", cdmData.getCDMVersion(), CDMVersion.CDM_2_0);
		assertTrue("Invalid CDM class instance.", cdmData instanceof CDMData_2_0);
		CDMData_2_0 typedCdmData = (CDMData_2_0) cdmData;
		assertEquals("Invalid model version.", "1.1", typedCdmData.getModelVersion());
		
		cdmData.applyToElement(testElement);
		assertEquals("Missing or wrong modelVersion attribute.", "1.1", testElement.getAttributeValue("modelVersion"));
	}
	
	@Test
	public void testFullCDM_2_0_Build() {
		CDMDataBuilder builder = new CDMDataBuilder(CDMVersion.CDM_2_0);
		builder.setModelVersion("1.1");
		builder.setCustomId("12345");
		builder.setPublisher("me");
		builder.setRef("8eafa5e6-be54-43b2-8152-ab072f54ee3f");
		builder.setUpdates("ccbcdd3e-3405-4145-b241-75233b431fe0");
		builder.setCopyOf("7db4b9f1-6859-4803-878f-c22c18a30335");
		List<Reference> referencesList = new ArrayList<Reference>();
		referencesList.add(new Reference("0e888c82-9197-4c11-8257-ae214201044a"));
		builder.setReferences(new References(referencesList));
		builder.addReference(new Reference("0232bd06-cd91-444e-bb36-b2cae450f48f", ReferenceType.WEAK));
		builder.setSummary(new Summary("My Summary."));
		builder.setCreationInfo(new CreationInfo(testDate, "me", "myapp"));
		
		CDMData cdmData;
		try {
			cdmData = builder.build();
		} catch (InvalidBuildException e) {
			fail("Build of full CDM 2.0 failed: " + e.getMessage());
			return;
		}
		assertEquals("Invalid CDM version was set internally.", cdmData.getCDMVersion(), CDMVersion.CDM_2_0);
		assertTrue("Invalid CDM instance.", cdmData instanceof CDMData_2_0);
		CDMData_2_0 typedCdmData = (CDMData_2_0) cdmData;
		assertEquals("Invalid model version.", "1.1", typedCdmData.getModelVersion());
		assertEquals("Invalid custom id.", "12345", typedCdmData.getCustomId());
		assertEquals("Invalid publisher.", "me", typedCdmData.getPublisher());
		assertEquals("Invalid ref.", "8eafa5e6-be54-43b2-8152-ab072f54ee3f", typedCdmData.getRef());
		assertEquals("Invalid updated object data.", "ccbcdd3e-3405-4145-b241-75233b431fe0", typedCdmData.getUpdatedObjectId());
		assertEquals("Invalid copy information.", "7db4b9f1-6859-4803-878f-c22c18a30335", typedCdmData.getCopyOf());
		References references = typedCdmData.getReferences();
		assertEquals("Invalid references count.", 2, references.getReferences().size());
		Reference reference1 = references.getReferences().get(0);
		assertEquals("Invalid reference id.", "0e888c82-9197-4c11-8257-ae214201044a", reference1.getId());
		assertEquals("Invalid reference type.", ReferenceType.DEPENDENCY, reference1.getReferenceType());
		Reference reference2 = references.getReferences().get(1);
		assertEquals("Invalid reference id.", "0232bd06-cd91-444e-bb36-b2cae450f48f", reference2.getId());
		assertEquals("Invalid reference type.", ReferenceType.WEAK, reference2.getReferenceType());
		Summary summary = typedCdmData.getSummary();
		assertEquals("Invalid summary.", "My Summary.", summary.getSummary());
		CreationInfo creationInfo = typedCdmData.getCreationInfo();
		assertEquals("Invalid creation date in info object.", testDate, creationInfo.getCreationDate());
		assertEquals("Invalid creator in info object.", "me", creationInfo.getCreator());
		assertEquals("Invalid application in creation info object.", "myapp", creationInfo.getApplication());
		
		cdmData.applyToElement(testElement);

		assertEquals("Missing or wrong cdmVersion attribute.", "2.0", testElement.getAttributeValue("cdmVersion"));
		assertEquals("Missing or wrong modelVersion attribute.", "1.1", testElement.getAttributeValue("modelVersion"));
		assertEquals("Missing or wrong customId attribute.", "12345", testElement.getAttributeValue("customId"));
		assertEquals("Missing or wrong publisher attribute.", "me", testElement.getAttributeValue("publisher"));
		assertEquals("Missing or wrong ref attribute.", "8eafa5e6-be54-43b2-8152-ab072f54ee3f", testElement.getAttributeValue("ref"));
		assertEquals("Missing or wrong updates attribute.", "ccbcdd3e-3405-4145-b241-75233b431fe0", testElement.getAttributeValue("updates"));
		assertEquals("Missing or wrong copyOf attribute.", "7db4b9f1-6859-4803-878f-c22c18a30335", testElement.getAttributeValue("copyOf"));
		
		Element referencesElement = testElement.getChild("references", testElement.getNamespace());
		assertNotNull("No references element found.", referencesElement);
		assertEquals("References element: Wrong number of child elements.", 2, referencesElement.getChildren().size());
		Element referenceElement1 = referencesElement.getChildren("reference", testElement.getNamespace()).get(0);
		assertEquals("References element (1): Wrong ID.", "0e888c82-9197-4c11-8257-ae214201044a", referenceElement1.getAttributeValue("id"));
		assertTrue("References element (1): Wrong type.", referenceElement1.getAttributeValue("type") == null || "dependecy".equals(referenceElement1.getAttributeValue("type")));
		Element referenceElement2 = referencesElement.getChildren("reference", testElement.getNamespace()).get(1);
		assertEquals("References element (2): Wrong ID.", "0232bd06-cd91-444e-bb36-b2cae450f48f", referenceElement2.getAttributeValue("id"));
		assertEquals("References element (2): Wrong type.", "weak", referenceElement2.getAttributeValue("type"));
		References restoredReferences = new References(referencesElement);
		String originalXMLString = xmlOutputter.outputString(referencesElement);
		String generatedXMLString = xmlOutputter.outputString(restoredReferences.generateXMLElement(referencesElement.getNamespaceURI()));
		assertEquals("References element: XML encoding or decoding failed.", originalXMLString, generatedXMLString);
		
		Element summaryElement = testElement.getChild("summary", testElement.getNamespace());
		assertNotNull("No summary element found.", summaryElement);
		assertEquals("Summary element: Wrong content.", "My Summary.", summaryElement.getText());
		Summary restoredSummary = new Summary(summaryElement);
		originalXMLString = xmlOutputter.outputString(summaryElement);
		generatedXMLString = xmlOutputter.outputString(restoredSummary.generateXMLElement(referencesElement.getNamespaceURI()));
		assertEquals("Summary element: XML encoding or decoding failed.", originalXMLString, generatedXMLString);
		
		Element creationInfoElement = testElement.getChild("creationInfo", testElement.getNamespace());
		assertNotNull("No creation info element found.", creationInfoElement);
		assertEquals("Creation info element: Wrong date.", "1980-08-15T11:32:57+02:00", creationInfoElement.getChild("date", testElement.getNamespace()).getText());
		assertEquals("Creation info element: Wrong person.", "me", creationInfoElement.getChild("person", testElement.getNamespace()).getText());
		assertEquals("Creation info element: Wrong application.", "myapp", creationInfoElement.getChild("application", testElement.getNamespace()).getText());
		
		CreationInfo restoredCreationInfo = new CreationInfo(creationInfoElement);
		originalXMLString = xmlOutputter.outputString(creationInfoElement);
		generatedXMLString = xmlOutputter.outputString(restoredCreationInfo.generateXMLElement(referencesElement.getNamespaceURI()));
		assertEquals("Summary element: XML encoding or decoding failed.", originalXMLString, generatedXMLString);
	}	

}
