package de.imc.mirror.sdk.java.cdm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.cdm.Reference.ReferenceType;
import de.imc.mirror.sdk.java.CDMDataBuilder;
import de.imc.mirror.sdk.java.exceptions.InvalidBuildException;

public class CDMData_2_0Test {
	private Date testDate;
	private XMLOutputter xmlOutputter; 
	
	@Before
	public void initializeTest() {
		testDate = new Date(335179977000l);
		xmlOutputter = new XMLOutputter();
	}

	@Test
	public void testSerialization() {
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
		
		CDMData_2_0 originalCdmData;
		try {
			originalCdmData = (CDMData_2_0) builder.build();
		} catch (InvalidBuildException e) {
			fail("Build of full CDM 2.0 failed: " + e.getMessage());
			return;
		}
		
		assertTrue("Invalid CDM instance.", originalCdmData instanceof CDMData_2_0);
		
		byte[] serializedCdmData;
		try {
			ByteOutputStream byteOutput = new ByteOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeObject(originalCdmData);
			objectOutput.close();
			serializedCdmData = byteOutput.getBytes();
			byteOutput.close();
		} catch (IOException e) {
			fail("Failed do write output stream.");
			return;
		}
		
		CDMData_2_0 restoredCdmData;
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(serializedCdmData);
			ObjectInputStream objectInput = new ObjectInputStream(byteInput);
			restoredCdmData = (CDMData_2_0) objectInput.readObject(); 
			objectInput.close();
			byteInput.close();
		} catch (IOException e) {
			e.printStackTrace();
			fail("Failed do read input stream.");
			return;
		} catch (ClassNotFoundException e) {
			fail("Failed to restore class.");
			return;
		}
		
		Element testElement = new Element("testElement", "test:namespace");
		originalCdmData.applyToElement(testElement);
		String originalCDMDataXML = xmlOutputter.outputString(testElement);
		testElement = new Element("testElement", "test:namespace");
		restoredCdmData.applyToElement(testElement);
		String restoredCDMDataXML = xmlOutputter.outputString(testElement);
		
		assertEquals("XML representations differ between serialized and deserialized objects.", originalCDMDataXML, restoredCDMDataXML);
	}
}
