package de.imc.mirror.sdk.java;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import de.imc.mirror.sdk.ConnectionStatus;
import de.imc.mirror.sdk.exceptions.ConnectionStatusException;
import static org.junit.Assert.*;

public class ConnectionHandlerTest {
	private static final String PROPERTIES_FILE = "test/connection.properties";
	Properties props;
	ConnectionConfiguration connectionConfig;
	
	public ConnectionHandlerTest() {
	}
	
	@Before
	public void prepare() throws FileNotFoundException, IOException {
		props = new Properties();
		props.load(new FileInputStream(PROPERTIES_FILE));
	}
	
	@Test
	public void testDefaultConnection() {
		ConnectionConfigurationBuilder configBuilder = new ConnectionConfigurationBuilder(props.getProperty("xmpp.domain"), props.getProperty("xmpp.appid"));
		ConnectionConfiguration connectionConfig = configBuilder.build();
		ConnectionHandler connectionHandler = new ConnectionHandler(
				props.getProperty("test.user.01.id"),
				props.getProperty("test.user.01.password"),
				connectionConfig);
		assertEquals("Intialized connection handler has wrong status.", ConnectionStatus.OFFLINE, connectionHandler.getStatus());
		try {
			connectionHandler.connect();
		} catch (ConnectionStatusException e) {
			e.printStackTrace();
			fail("Failed to establish connection: " + e.getMessage());
		}
		assertEquals("Established connection but proper status is not set.", ConnectionStatus.ONLINE, connectionHandler.getStatus());
		connectionHandler.disconnect();
		assertEquals("Closed connection but proper status is not set.", ConnectionStatus.OFFLINE, connectionHandler.getStatus());
	}
	
	@Test
	public void testNetworkInformation() {
		ConnectionConfigurationBuilder configBuilder = new ConnectionConfigurationBuilder(props.getProperty("xmpp.domain"), props.getProperty("xmpp.appid"));
		ConnectionConfiguration connectionConfig = configBuilder.build();
		ConnectionHandler connectionHandler = new ConnectionHandler(
				props.getProperty("test.user.01.id"),
				props.getProperty("test.user.01.password"),
				connectionConfig);
		try {
			connectionHandler.connect();
		} catch (ConnectionStatusException e) {
			e.printStackTrace();
			fail("Failed to establish connection: " + e.getMessage());
		}
		NetworkInformation networkInformation = connectionHandler.getNetworkInformation();
		assertEquals("Retrieved unexpected JID for the spaces service.", props.getProperty("msf.spaces-service.subdomain") + "." + props.getProperty("xmpp.domain"), networkInformation.getSpacesServiceJID());
		assertEquals("Retrieved unexpected version for the spaces service.", props.getProperty("msf.spaces-service.version"), networkInformation.getSpacesServiceVersion());
		assertEquals("Retrieved unexpected JID for the persistence service.", props.getProperty("msf.persistence-service.subdomain") + "." + props.getProperty("xmpp.domain"), networkInformation.getPersistenceServiceJID());
		connectionHandler.disconnect();
	}
}
