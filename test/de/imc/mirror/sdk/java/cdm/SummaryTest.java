package de.imc.mirror.sdk.java.cdm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class SummaryTest {
	XMLOutputter xmlOutputter;
	
	@Before
	public void initializeTest() {
		xmlOutputter = new XMLOutputter();
	}
	
	@Test
	public void testSerialization() {
		Summary originalSummary = new Summary("My Summary.");
		
		String serializedSummary;
		try {
			ByteOutputStream byteOutput = new ByteOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeObject(originalSummary);
			objectOutput.close();
			serializedSummary = byteOutput.toString();
			byteOutput.close();
		} catch (IOException e) {
			fail("Failed do write output stream.");
			return;
		}
		
		Summary restoredSummary;
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(serializedSummary.getBytes());
			ObjectInputStream objectInput = new ObjectInputStream(byteInput);
			restoredSummary = (Summary) objectInput.readObject(); 
			objectInput.close();
			byteInput.close();
		} catch (IOException e) {
			fail("Failed do read input stream.");
			return;
		} catch (ClassNotFoundException e) {
			fail("Failed to restore class.");
			return;
		}
		
		assertEquals("Model version differs.", originalSummary.getSummary(), restoredSummary.getSummary());
		assertEquals("XML representation differs.",
				xmlOutputter.outputString(originalSummary.generateXMLElement("test:namespace")),
				xmlOutputter.outputString(restoredSummary.generateXMLElement("test:namespace")));
	}
}
