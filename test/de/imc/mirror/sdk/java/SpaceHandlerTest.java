package de.imc.mirror.sdk.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.imc.mirror.sdk.OfflineModeHandler.Mode;
import de.imc.mirror.sdk.Space.PersistenceType;
import de.imc.mirror.sdk.Space.Type;
import de.imc.mirror.sdk.SpaceMember.Role;
import de.imc.mirror.sdk.exceptions.ConnectionStatusException;
import de.imc.mirror.sdk.exceptions.SpaceManagementException;
import de.imc.mirror.sdk.exceptions.UnknownEntityException;
import de.imc.mirror.sdk.java.data.DatabaseConfig;

public class SpaceHandlerTest {
	private static final String PROPERTIES_FILE = "test/connection.properties";
	
	private Properties props;
	private ConnectionHandler connectionHandler;
	private SpaceHandler spaceHandler;
	
	@Before
	public void initialize() throws FileNotFoundException, IOException, ConnectionStatusException {
		props = new Properties();
		props.load(new FileInputStream(PROPERTIES_FILE));
		ConnectionConfigurationBuilder configBuilder = new ConnectionConfigurationBuilder(props.getProperty("xmpp.domain"), props.getProperty("xmpp.appid"));
		ConnectionConfiguration connectionConfig = configBuilder.build();
		connectionHandler = new ConnectionHandler(props.getProperty("test.user.01.id"), props.getProperty("test.user.01.password"), connectionConfig);
		connectionHandler.connect();
		spaceHandler = new SpaceHandler(connectionHandler, DatabaseConfig.disableDatabaseCaching());
		spaceHandler.setMode(Mode.ONLINE);
	}
	
	@After
	public void destroy() {
		connectionHandler.disconnect();
	}
	
	@Ignore
	@Test
	public void testMethodChangeConnectionHandler() {
		// TODO Implement test.
		fail("Test is not implemented.");
	}
	
	@Ignore
	@Test
	public void testMethodClear() {
		// TODO Implement test.
		fail("Test is not implemented.");
	}
	
	private String generateBareJID(String userId) {
		return userId + "@" + connectionHandler.getConfiguration().getDomain();
	}
	
	@Test
	public void testMethodConfigureSpace() {
		String testSpaceId = props.getProperty("test.space.team.id");
		
		Space testSpace = (Space) spaceHandler.getSpace(testSpaceId);
		assertNotNull("Test space " + testSpaceId + " is not available.", testSpace);
		
		try {
			// Apply initial configuration.
			SpaceConfiguration spaceConfig = new SpaceConfiguration();
			spaceConfig.setName("[TESTING]");
			spaceConfig.setType(Type.TEAM);
			spaceConfig.setPersistenceType(PersistenceType.OFF);
			spaceConfig.setPersistenceDuration(null);
			Set<de.imc.mirror.sdk.SpaceMember> members = new HashSet<de.imc.mirror.sdk.SpaceMember>();
			members.add(new SpaceMember(connectionHandler.getCurrentUser().getFullJID(), Role.MODERATOR));
			spaceConfig.setMembers(members);
			testSpace = (Space) spaceHandler.configureSpace(testSpaceId, spaceConfig);
			
			// Check initial configuration.
			assertEquals("Space name does not match the initial configuration.", "[TESTING]", testSpace.getName());
			assertEquals("Space type does not match the initial configuration.", Type.TEAM, testSpace.getType());
			assertEquals("Space persistence type does not match the initial configuration.", PersistenceType.OFF, testSpace.getPersistenceType());
			assertNull("Space persistence duration does not match the initial configuration.", testSpace.getPersistenceDuration());
			assertEquals("Number of space members does not match the initial configuration.", 1, testSpace.getMembers().size());
			SpaceMember spaceMember = (SpaceMember) testSpace.getMembers().iterator().next();
			assertEquals("Space member JID does not match the initial configuration.", connectionHandler.getCurrentUser().getBareJID(), spaceMember.getJID());
			assertEquals("Space member role does not match the initial configuration.", Role.MODERATOR, spaceMember.getRole());
			
			// Change space name.
			String newSpaceName = UUID.randomUUID().toString();
			spaceConfig.setName(newSpaceName);
			testSpace = (Space) spaceHandler.configureSpace(testSpaceId, spaceConfig);
			assertEquals("Space name does not mtach the updated configuration.", newSpaceName, testSpace.getName());
			
			// Change persistence type.
			spaceConfig.setPersistenceType(PersistenceType.ON);
			testSpace = (Space) spaceHandler.configureSpace(testSpaceId, spaceConfig);
			assertEquals("Space persistence type does not match the updated configuration.", PersistenceType.ON, testSpace.getPersistenceType());
			spaceConfig.setPersistenceType(PersistenceType.ON);
			
			// Set persistence duration.
			spaceConfig.setPersistenceType(PersistenceType.DURATION);
			Duration duration = null;
			try {
				duration = DatatypeFactory.newInstance().newDurationDayTime(true, 5, 7, 11, 13);
			} catch (DatatypeConfigurationException e) {
				fail("Failed to create duration instance: " + e.getMessage());
			}
			spaceConfig.setPersistenceDuration(duration);
			testSpace = (Space) spaceHandler.configureSpace(testSpaceId, spaceConfig);
			assertEquals("Space persistence type does not match the updated configuration.", PersistenceType.DURATION, testSpace.getPersistenceType());
			assertEquals("Space persistence duration does not match the updated configuration.", duration, testSpace.getPersistenceDuration());
			
			// Change space members.
			String testUser01JID = generateBareJID(props.getProperty("test.user.01.id"));
			String testUser02JID = generateBareJID(props.getProperty("test.user.02.id"));
			String testUser03JID = generateBareJID(props.getProperty("test.user.03.id"));
			spaceConfig.addMember(new SpaceMember(testUser02JID, Role.MODERATOR));
			spaceConfig.addMember(new SpaceMember(testUser03JID, Role.MEMBER));
			testSpace = (Space) spaceHandler.configureSpace(testSpaceId, spaceConfig);
			assertEquals("Number of space members does not match the updated configuration.", 3, testSpace.getMembers().size());
			Iterator<de.imc.mirror.sdk.SpaceMember> spaceMemberIterator = testSpace.getMembers().iterator();
			while (spaceMemberIterator.hasNext()) {
				SpaceMember member = (SpaceMember) spaceMemberIterator.next();
				if (member.getJID().equals(testUser01JID)) {
					assertEquals("Space member role does not match the updated configuration.", Role.MODERATOR, member.getRole());
				} else if (member.getJID().equals(testUser02JID)) {
					assertEquals("Space member role does not match the updated configuration.", Role.MODERATOR, member.getRole());
				} else if (member.getJID().equals(testUser03JID)) {
					assertEquals("Space member role does not match the updated configuration.", Role.MEMBER, member.getRole());
				} else {
					fail("Space member JID does not match the updated configuration");
				}
			}
			
			spaceConfig.removeMember(testUser02JID);
			testSpace = (Space) spaceHandler.configureSpace(testSpaceId, spaceConfig);
			assertEquals("Number of space members does not match the updated configuration.", 2, testSpace.getMembers().size());
			assertFalse("Wrong space member was removed.", testSpace.getMembers().contains(new SpaceMember(testUser02JID, Role.MODERATOR)));
		} catch (SpaceManagementException e) {
			fail("Failed to apply space configuration: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Invalid connection status: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodCreateDefaultSpace() {
		String spaceId = connectionHandler.getCurrentUser().getUsername();

		// Delete private space.
		try {
			spaceHandler.deleteSpace(spaceId);
			assertNull("Default space is still available after deletion.", spaceHandler.getDefaultSpace());
			
			// Create default (private) space.
			spaceHandler.createDefaultSpace();
			Space defaultSpace = (Space) spaceHandler.getDefaultSpace(); 
			assertNotNull("Default space was not created.", defaultSpace);
			assertEquals("Default space is not private.", Type.PRIVATE, defaultSpace.getType());
		} catch (SpaceManagementException e) {
			fail("Private space could not be deleted: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodCreateSpace() {
		String testUser02JID = generateBareJID(props.getProperty("test.user.02.id"));
		String testUser03JID = generateBareJID(props.getProperty("test.user.03.id"));

		String spaceName = UUID.randomUUID().toString();
		Type spaceType = new Type[]{Type.TEAM, Type.ORGA}[new Random().nextInt(2)];
		SpaceMember firstMember = new SpaceMember(connectionHandler.getCurrentUser().getBareJID(), Role.MODERATOR);
		String spaceMemberJID = new String[] {testUser02JID, testUser03JID}[new Random().nextInt(2)];
		Role spaceMemberRole = Role.values()[new Random().nextInt(2)];
		SpaceMember secondMember = new SpaceMember(spaceMemberJID, spaceMemberRole);
		PersistenceType spacePersistenceType = PersistenceType.values()[new Random().nextInt(PersistenceType.values().length)];
		DatatypeFactory datatypeFactory = null;
		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			fail("Failed to instantiate data type factory: " + e.getMessage());
		}
		Duration spacePersistenceDuration = (spacePersistenceType == PersistenceType.DURATION) ? datatypeFactory.newDuration(new Random().nextLong()) : null;
		
		SpaceConfiguration spaceConfig = new SpaceConfiguration();
		spaceConfig.setName(spaceName);
		spaceConfig.setType(spaceType);
		spaceConfig.addMember(firstMember);
		spaceConfig.addMember(secondMember);
		spaceConfig.setPersistenceType(spacePersistenceType);
		spaceConfig.setPersistenceDuration(spacePersistenceDuration);
		
		System.out.println("testMethodCreateSpace() - Generated space configuration: " + spaceConfig);
		
		try {
			Space createdSpace = (Space) spaceHandler.createSpace(spaceConfig);
			String spaceId = createdSpace.getId();
			assertEquals("Space name does not match with configuration.", spaceName, createdSpace.getName());
			assertEquals("Space type does not match with configuration.", spaceType, createdSpace.getType());
			assertEquals("Space member count does not match with configuration.", 2, createdSpace.getMembers().size());
			assertTrue("Current user is not moderator of the space.", createdSpace.getMembers().contains(firstMember));
			assertTrue("Additional user is member of the space of has wrong role.", createdSpace.getMembers().contains(secondMember));
			assertEquals("Space persistence type does not match with configuration.", spacePersistenceType, createdSpace.getPersistenceType());
			assertEquals("Space persistence duration does not match with configuration.", spacePersistenceDuration, createdSpace.getPersistenceDuration());
			
			spaceHandler.deleteSpace(spaceId);
			assertNull("Created space could not be deleted.", spaceHandler.getSpace(spaceId));
		} catch (SpaceManagementException e) {
			fail("Failed to create new space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		}
		
	}
	
	@Test
	public void testMethodDeleteSpace() {
		String spaceName = UUID.randomUUID().toString();
		SpaceConfiguration spaceConfig = new SpaceConfiguration(Type.TEAM, spaceName, connectionHandler.getCurrentUser().getBareJID(), PersistenceType.OFF, null);
		try {
			Space createdSpace = (Space) spaceHandler.createSpace(spaceConfig);
			String spaceId = createdSpace.getId();
			spaceHandler.deleteSpace(spaceId);
			assertNull("Failed to delete space.", spaceHandler.getSpace(spaceId));
		} catch (SpaceManagementException e) {
			fail("Failed to create or delete new space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodGetAllSpaces() {
		String testSpaceId = props.getProperty("test.space.team.id");
		
		List<de.imc.mirror.sdk.Space> spaces = spaceHandler.getAllSpaces();
		assertNotNull("Spaces list is null.", spaces);
		
		Map<String, String> spacesList = spaceHandler.getSpacesList();
		ArrayList<String> spaceIds = new ArrayList<String>(spacesList.keySet());
		
		boolean containsTestSpace = false;
		System.out.println("testMethodGetAllSpaces() - Spaces retrieved:");
		for (de.imc.mirror.sdk.Space space : spaces) {
			System.out.println(" - " + space.getId());
			if (!spaceIds.remove(space.getId())) {
				fail("Space " + space.getId() + " is missing in the spaces list.");
			}
			if (space.getId().equals(testSpaceId)) {
				containsTestSpace = true;
			}
		}
		assertTrue("List of spaces does not contain test space (" + testSpaceId + ")", containsTestSpace);
		assertTrue("List of spaces does not contain all spaces listed with getSpacesList(). Remains: " + spaceIds, spaceIds.isEmpty());
	}
	
	@Test
	public void testMethodGetDefaultSpace() {
		try {
			// Delete default space (i.e. private space of the current user).
			spaceHandler.deleteSpace(connectionHandler.getCurrentUser().getUsername());
			Space defaultSpace = spaceHandler.getDefaultSpace();
			assertNull("Returned default space although it was deleted.", defaultSpace);
			defaultSpace = spaceHandler.createDefaultSpace();
			assertNotNull("Failed to retrieve default space.", defaultSpace);
			assertEquals("Default space is not the private space.", Type.PRIVATE, defaultSpace.getType());
		} catch (SpaceManagementException e) {
			fail("Failed to create default space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodGetMode() {
		assertEquals("Space handler is not online.", Mode.ONLINE, spaceHandler.getMode());
		spaceHandler.setMode(Mode.OFFLINE);
		assertEquals("Space handler is not offline as expected.", Mode.OFFLINE, spaceHandler.getMode());
	}
	
	@Test
	public void testMethodGetSpace() {
		String testSpaceId = props.getProperty("test.space.team.id");
		Space testSpace = (Space) spaceHandler.getSpace(testSpaceId);
		assertNotNull("Test space " + testSpaceId + " not found.", testSpace);
	}
	
	@Test
	public void testMethodGetSpacesList() {
		String testSpaceId = props.getProperty("test.space.team.id");
		Map<String, String> spacesList = spaceHandler.getSpacesList();
		assertTrue("List of spaces (" + spacesList.keySet() + ") does not contain test space (" + testSpaceId + ").", spacesList.containsKey(testSpaceId));
	}
	
	@Test
	public void testMethodIsModeratorOfSpace() {
		String testSpaceId = props.getProperty("test.space.team.id");
		String testUser01JID = generateBareJID(props.getProperty("test.user.01.id"));
		String testUser02JID = generateBareJID(props.getProperty("test.user.02.id"));
		
		
		Space testSpace = (Space) spaceHandler.getSpace(testSpaceId);
		SpaceConfiguration spaceConfig = testSpace.generateSpaceConfiguration();
		Set<de.imc.mirror.sdk.SpaceMember> spaceMembers = new HashSet<de.imc.mirror.sdk.SpaceMember>();
		spaceMembers.add(new SpaceMember(testUser01JID, Role.MODERATOR));
		spaceMembers.add(new SpaceMember(testUser02JID, Role.MEMBER));
		spaceConfig.setMembers(spaceMembers);
		
		try {
			testSpace = (Space) spaceHandler.configureSpace(testSpaceId, spaceConfig);
			assertTrue("Current user is not moderator of test space.", spaceHandler.isModeratorOfSpace(testUser01JID, testSpaceId));
			assertFalse("Second user should not be moderator of test space.", spaceHandler.isModeratorOfSpace(testUser02JID, testSpaceId));
		} catch (SpaceManagementException e) {
			fail("Failed to configure space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (UnknownEntityException e) {
			fail("Unknown space: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodSetMode() {
		spaceHandler.setMode(Mode.OFFLINE);
		assertEquals("Space handler did not switch to offline mode.", Mode.OFFLINE, spaceHandler.getMode());
		spaceHandler.setMode(Mode.ONLINE);
		assertEquals("Space handler did not switch to online mode.", Mode.ONLINE, spaceHandler.getMode());
	}
	
	@Test
	public void testMethodSetModelsSupportedBySpace() {
		SpaceConfiguration spaceConfig = new SpaceConfiguration(Type.ORGA, UUID.randomUUID().toString(), connectionHandler.getCurrentUser().getBareJID(), PersistenceType.OFF, null);
		DataModel pingModel = new DataModel("mirror:application:ping:ping", "http://data.mirror-demo.eu/application/ping/ping-1.0.xsd");
		DataModel moodModel = new DataModel("mirror:application:integratedmoodmap:mood", "http://data.mirror-demo.eu/application/integratedmoodmap/mood-1.0.xsd");
		Set<de.imc.mirror.sdk.DataModel> dataModels = new HashSet<de.imc.mirror.sdk.DataModel>();
		dataModels.add(pingModel);
		dataModels.add(moodModel);
		
		try {
			OrgaSpace testSpace = (OrgaSpace) spaceHandler.createSpace(spaceConfig);
			assertTrue("Initial list of supported data models is not empty.", testSpace.getSupportedDataModels().isEmpty());
			testSpace = (OrgaSpace) spaceHandler.setModelsSupportedBySpace(testSpace.getId(), dataModels);
			Set<de.imc.mirror.sdk.DataModel> retrievedDataModels = testSpace.getSupportedDataModels();
			assertEquals("Unexpected number of supported data models retrieved.", 2, retrievedDataModels.size());
			assertTrue("Ping data model is not contained.", retrievedDataModels.contains(pingModel));
			assertTrue("Mood data model is not contained.", retrievedDataModels.contains(moodModel));
			testSpace = (OrgaSpace) spaceHandler.setModelsSupportedBySpace(testSpace.getId(), new HashSet<de.imc.mirror.sdk.DataModel>());
			assertTrue("List of supported data models is not empty after reset.", testSpace.getSupportedDataModels().isEmpty());
		} catch (SpaceManagementException e) {
			fail("Failed to create orga space or apply model configuration: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		}
	}
	
	@Test
	public void testInteropDataHandler() {
		String testSpaceId = props.getProperty("test.space.team.id");
		
		for (int i = 0; i < 100; i++) {
			Space testSpace = (Space) spaceHandler.getSpace(testSpaceId);
			assertNotNull("Test space " + testSpaceId + " not found.", testSpace);
		}
		
		new DataHandler(connectionHandler, spaceHandler);
		
		for (int i = 0; i < 100; i++) {
			Space testSpace = (Space) spaceHandler.getSpace(testSpaceId);
			assertNotNull("Test space " + testSpaceId + " not found after data handler was initialized.", testSpace);
		}
	}
}
