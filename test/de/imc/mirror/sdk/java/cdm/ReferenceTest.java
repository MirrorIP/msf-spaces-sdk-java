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

import de.imc.mirror.sdk.cdm.Reference.ReferenceType;

public class ReferenceTest {
	XMLOutputter xmlOutputter;
	
	@Before
	public void initializeTest() {
		xmlOutputter = new XMLOutputter();
	}
	
	@Test
	public void testSerialization() {
		Reference originalReference = new Reference("8eafa5e6-be54-43b2-8152-ab072f54ee3f", ReferenceType.WEAK);
		
		String serializedReference;
		try {
			ByteOutputStream byteOutput = new ByteOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeObject(originalReference);
			objectOutput.close();
			serializedReference = byteOutput.toString();
			byteOutput.close();
		} catch (IOException e) {
			fail("Failed do write output stream.");
			return;
		}
		
		Reference restoredReference;
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(serializedReference.getBytes());
			ObjectInputStream objectInput = new ObjectInputStream(byteInput);
			restoredReference = (Reference) objectInput.readObject(); 
			objectInput.close();
			byteInput.close();
		} catch (IOException e) {
			fail("Failed do read input stream.");
			return;
		} catch (ClassNotFoundException e) {
			fail("Failed to restore class.");
			return;
		}
		
		assertEquals("XML representation differs.",
				xmlOutputter.outputString(originalReference.generateXMLElement("test:namespace")),
				xmlOutputter.outputString(restoredReference.generateXMLElement("test:namespace")));
	}
}
