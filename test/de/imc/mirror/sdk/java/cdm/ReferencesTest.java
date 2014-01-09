package de.imc.mirror.sdk.java.cdm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.imc.mirror.sdk.cdm.Reference.ReferenceType;

public class ReferencesTest {
	XMLOutputter xmlOutputter;
	
	@Before
	public void initializeTest() {
		xmlOutputter = new XMLOutputter();
	}
	
	@Test
	public void testSerialization() {
		List<Reference> referencesList = new ArrayList<Reference>();
		referencesList.add(new Reference("8eafa5e6-be54-43b2-8152-ab072f54ee3f"));
		referencesList.add(new Reference("8eafa5e6-be54-43b2-8152-ab072f54ee3d", ReferenceType.DEPENDENCY));
		References originalReferences = new References(referencesList);
		
		String serializedReferences;
		try {
			ByteOutputStream byteOutput = new ByteOutputStream();
			ObjectOutputStream objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeObject(originalReferences);
			objectOutput.close();
			serializedReferences = byteOutput.toString();
			byteOutput.close();
		} catch (IOException e) {
			fail("Failed do write output stream.");
			return;
		}
		
		References restoredReferences;
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(serializedReferences.getBytes());
			ObjectInputStream objectInput = new ObjectInputStream(byteInput);
			restoredReferences = (References) objectInput.readObject(); 
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
				xmlOutputter.outputString(originalReferences.generateXMLElement("test:namespace")),
				xmlOutputter.outputString(restoredReferences.generateXMLElement("test:namespace")));
	}
}
