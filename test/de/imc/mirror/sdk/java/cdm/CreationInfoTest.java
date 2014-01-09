package de.imc.mirror.sdk.java.cdm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

public class CreationInfoTest {
	XMLOutputter xmlOutputter;
	
	@Before
	public void initializeTest() {
		xmlOutputter = new XMLOutputter();
	}
	
	@Test
	public void testSerialization() {
		CreationInfo originalCreationInfo = new CreationInfo(new Date(), "me", "myapp");
		
		String serializedCreationInfo;
		try {
			ByteOutputStream byteOutput = new ByteOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeObject(originalCreationInfo);
			objectOutput.close();
			serializedCreationInfo = byteOutput.toString();
			byteOutput.close();
		} catch (IOException e) {
			fail("Failed do write output stream.");
			return;
		}
		
		CreationInfo restoredCreationInfo;
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(serializedCreationInfo.getBytes());
			ObjectInputStream objectInput = new ObjectInputStream(byteInput);
			restoredCreationInfo = (CreationInfo) objectInput.readObject(); 
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
		
		assertEquals("XML representation differs.",
				xmlOutputter.outputString(originalCreationInfo.generateXMLElement("test:namespace")),
				xmlOutputter.outputString(restoredCreationInfo.generateXMLElement("test:namespace")));
	}
}
