package de.imc.mirror.sdk.java.cdm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Before;
import org.junit.Test;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.imc.mirror.sdk.java.cdm.CDMData_1_0;
import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.java.CDMDataBuilder;
import de.imc.mirror.sdk.java.exceptions.InvalidBuildException;

public class CDMData_1_0Test {
	
	@Before
	public void initializeTest() {
	}
	
	@Test
	public void testSerialization() {
		CDMDataBuilder builder = new CDMDataBuilder(CDMVersion.CDM_1_0);
		builder.setModelVersion("1.1");
		builder.setCustomId("12345");
		builder.setPublisher("me");
		builder.setRef("8eafa5e6-be54-43b2-8152-ab072f54ee3f");
		
		CDMData_1_0 originalCdmData;
		try {
			originalCdmData = (CDMData_1_0) builder.build();
		} catch (InvalidBuildException e) {
			fail("Build of full CDM 1.0 failed: " + e.getMessage());
			return;
		}
		
		assertTrue("Invalid CDM instance.", originalCdmData instanceof CDMData_1_0);
		
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
		
		CDMData_1_0 restoredCdmData;
		try {
			ByteArrayInputStream byteInput = new ByteArrayInputStream(serializedCdmData);
			ObjectInputStream objectInput = new ObjectInputStream(byteInput);
			restoredCdmData = (CDMData_1_0) objectInput.readObject(); 
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
		
		assertEquals("Model version differs.", originalCdmData.getModelVersion(), restoredCdmData.getModelVersion());
		assertEquals("CDM version differs.", originalCdmData.getCDMVersion(), restoredCdmData.getCDMVersion());
		assertEquals("Custom identifier differs.", originalCdmData.getCustomId(), restoredCdmData.getCustomId());
		assertEquals("Publisher differs.", originalCdmData.getPublisher(), restoredCdmData.getPublisher());
		assertEquals("Ref differs.", originalCdmData.getRef(), restoredCdmData.getRef());
	}
}
