package de.imc.mirror.sdk.java;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.DataObjectFilter;
import de.imc.mirror.sdk.DataObjectListener;
import de.imc.mirror.sdk.OfflineModeHandler.Mode;
import de.imc.mirror.sdk.SerializableDataObjectFilter;
import de.imc.mirror.sdk.Space.PersistenceType;
import de.imc.mirror.sdk.Space.Type;
import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.exceptions.ConnectionStatusException;
import de.imc.mirror.sdk.exceptions.InvalidDataException;
import de.imc.mirror.sdk.exceptions.QueryException;
import de.imc.mirror.sdk.exceptions.SpaceManagementException;
import de.imc.mirror.sdk.exceptions.UnknownEntityException;
import de.imc.mirror.sdk.java.data.DatabaseConfig;
import de.imc.mirror.sdk.java.exceptions.InvalidBuildException;
import de.imc.mirror.sdk.java.filter.NamespaceFilter;
import de.imc.mirror.sdk.java.filter.PeriodFilter;

public class DataHandlerTest {
	private static final String PROPERTIES_FILE = "test/connection.properties";
	
	private Properties props;
	private ConnectionHandler connectionHandler;
	private SpaceHandler spaceHandler;
	private Space privateSpace;
	private DataHandler dataHandler;
	
	@Before
	public void initialize() throws FileNotFoundException, IOException, ConnectionStatusException, SpaceManagementException {
		props = new Properties();
		props.load(new FileInputStream(PROPERTIES_FILE));
		ConnectionConfigurationBuilder configBuilder = new ConnectionConfigurationBuilder(props.getProperty("xmpp.domain"), props.getProperty("xmpp.appid"));
		ConnectionConfiguration connectionConfig = configBuilder.build();
		connectionHandler = new ConnectionHandler(props.getProperty("test.user.01.id"), props.getProperty("test.user.01.password"), connectionConfig);
		connectionHandler.connect();
		spaceHandler = new SpaceHandler(connectionHandler, DatabaseConfig.disableDatabaseCaching());
		spaceHandler.setMode(Mode.ONLINE);
		
		// Apply default configuration to private space.
		privateSpace = spaceHandler.getDefaultSpace();
		if (privateSpace == null) {
			privateSpace = spaceHandler.createDefaultSpace();
		}
		SpaceConfiguration spaceConfig = new SpaceConfiguration(Type.PRIVATE, "[TESTING] private", connectionHandler.getCurrentUser().getBareJID(), PersistenceType.OFF, null);
		spaceHandler.configureSpace(privateSpace.getId(), spaceConfig);
		
		dataHandler = new DataHandler(connectionHandler, spaceHandler);
		dataHandler.setMode(Mode.ONLINE);
	}
	
	@After
	public void destroy() {
		connectionHandler.disconnect();
	}
	
	private DataObject generatePingObject(String customId) {
		CDMDataBuilder cdmBuilder = new CDMDataBuilder(CDMVersion.CDM_1_0);
		cdmBuilder.setModelVersion("1.0");
		cdmBuilder.setCustomId(customId);
		DataObjectBuilder objectBuilder = new DataObjectBuilder("ping", "mirror:application:ping:ping");
		CDMData cdmData = null;
		try {
			cdmData = cdmBuilder.build();
		} catch (InvalidBuildException e) {
			fail("Failed to build CDM data: " + e.getMessage());
		}
		objectBuilder.setCDMData(cdmData);
		return objectBuilder.build();
	}
	
	private DataObject generateInvalidMoodObject() {
		CDMDataBuilder cdmBuilder = new CDMDataBuilder(CDMVersion.CDM_1_0);
		cdmBuilder.setModelVersion("1.0");
		DataObjectBuilder objectBuilder = new DataObjectBuilder("mood", "mirror:application:integratedmoodmap:mood");
		CDMData cdmData = null;
		try {
			cdmData = cdmBuilder.build();
		} catch (InvalidBuildException e) {
			fail("Failed to build CDM data: " + e.getMessage());
		}
		objectBuilder.setCDMData(cdmData);
		return objectBuilder.build();
	}

	private void publishPing(String customId, String spaceId) {
		try {
			DataObject pingObject = generatePingObject(customId);
			dataHandler.publishDataObject(pingObject, spaceId);
		} catch (UnknownEntityException e) {
			fail("Unknown space: " + e.getMessage());
		} catch (InvalidDataException e) {
			fail("The data object was rejected: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodAddDataObjectListener() {
		String spaceId = connectionHandler.getCurrentUser().getUsername(); // private space
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		final RequestFuture<DataObject> requestFuture = new RequestFuture<DataObject>();
		
		try {
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register for space :" + e.getMessage());
		}
		
		// Register for private space.
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				requestFuture.setResponse(dataObject);
			}
		};
		dataHandler.addDataObjectListener(listener);
		
		publishPing(UUID.randomUUID().toString(), spaceId);
		try {
			requestFuture.get(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			fail("Publishing request was interrupted: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Execturion exception when performing publishing request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Failed to recieve response within " + timeout + " milliseconds.");
		}
	}
	 
	@Test
	public void testMethodPublishDataObject() {
		String spaceId = connectionHandler.getCurrentUser().getUsername(); // private space
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		String customId = UUID.randomUUID().toString(); 
		final RequestFuture<DataObject> requestFuture = new RequestFuture<DataObject>();
		
		try {
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register for space :" + e.getMessage());
		}
		
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				requestFuture.setResponse(dataObject);
			}
		};
		dataHandler.addDataObjectListener(listener);
		
		publishPing(customId, spaceId);
		try {
			DataObject receivedDataObject = requestFuture.get(timeout, TimeUnit.MILLISECONDS);
			assertEquals("Received data object does not contain correct custom id.", customId, receivedDataObject.getElement().getAttributeValue("customId"));
		} catch (InterruptedException e) {
			fail("Publishing request was interrupted: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Execturion exception when performing publishing request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Failed to recieve response within " + timeout + " milliseconds.");
		}
	}

	@Test(expected = TimeoutException.class)
	public void testMethodRemoveDataObjectListener() throws TimeoutException {
		String spaceId = connectionHandler.getCurrentUser().getUsername(); // private space
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		final RequestFuture<DataObject> requestFuture = new RequestFuture<DataObject>();
		
		try {
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register for space :" + e.getMessage());
		}
		
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				requestFuture.setResponse(dataObject);
			}
		};
		dataHandler.addDataObjectListener(listener);
		dataHandler.removeDataObjectListener(listener);
		
		publishPing(UUID.randomUUID().toString(), spaceId);
		try {
			requestFuture.get(timeout, TimeUnit.MILLISECONDS);
			fail("Received data object after the data object listener was removed.");
		} catch (InterruptedException e) {
			fail("Publishing request was interrupted: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Execturion exception when performing publishing request: " + e.getMessage());
		}
	}
	
	@Ignore
	@Test
	public void testMethodChangeConnectionHandler() {
		// TODO Implement 
	}
	
	@Test
	public void testMethodGetHandledSpaces() {
		Set<String> handledSpaces = new HashSet<String>();
		handledSpaces.add(connectionHandler.getCurrentUser().getUsername());
		handledSpaces.add(props.getProperty("test.space.team.id"));
		handledSpaces.add(props.getProperty("test.space.orga.id"));
		
		try {
			for (String spaceId : handledSpaces) {
				dataHandler.registerSpace(spaceId);
			}
			for (de.imc.mirror.sdk.Space space : dataHandler.getHandledSpaces()) {
				handledSpaces.remove(space.getId());
			}
			assertEquals("Not all registered spaces are handled. Missing: " + handledSpaces, 0, handledSpaces.size());
		} catch (UnknownEntityException e) {
			fail("Failed to register for space :" + e.getMessage());
		}
	}
	
	@Ignore
	@Test
	public void testMethodRegisterSpace() {
		// Already implemented by testMethodGetHandledSpaces & testMethodAddDataObjectListener.
	}
	
	@Test
	public void testMethodRemoveSpace() {
		Set<String> handledSpaces = new HashSet<String>();
		handledSpaces.add(connectionHandler.getCurrentUser().getUsername());
		String teamSpaceId = props.getProperty("test.space.team.id");
		handledSpaces.add(teamSpaceId);
		handledSpaces.add(props.getProperty("test.space.orga.id"));
		
		try {
			for (String spaceId : handledSpaces) {
				dataHandler.registerSpace(spaceId);
			}
			dataHandler.removeSpace(teamSpaceId);
			List<de.imc.mirror.sdk.Space> retrievedSpacesHandled = dataHandler.getHandledSpaces();
			for (de.imc.mirror.sdk.Space space : retrievedSpacesHandled) {
				System.out.println("Handled space: " + space.getId());
				if (teamSpaceId.equals(space.getId())) {
					fail("Removed space is still handled: " + teamSpaceId);
				}
			}
			assertEquals("Invalid number of handled spaces.", 2, retrievedSpacesHandled.size());
		} catch (UnknownEntityException e) {
			fail("Failed to register for space :" + e.getMessage());
		}
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testMethodRetrieveDataObjects() {
		String spaceId = props.getProperty("test.space.team.id"); // connectionHandler.getCurrentUser().getUsername(); // private space
		String customId = UUID.randomUUID().toString(); 
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		
		Space space = (Space) spaceHandler.getSpace(spaceId);
		SpaceConfiguration spaceConfiguration = space.generateSpaceConfiguration();
		spaceConfiguration.setPersistenceType(PersistenceType.ON);
		
		try {
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register test listener. Space: " + spaceId);
		}
		
		final RequestFuture<DataObject> requestFuture = new RequestFuture<DataObject>();
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				requestFuture.setResponse(dataObject);
			}
		};
		
		dataHandler.addDataObjectListener(listener);
		try {
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register for space :" + e.getMessage());
		}
		
		try {
			dataHandler.registerSpace(spaceId);
			spaceHandler.configureSpace(spaceId, spaceConfiguration);
			publishPing(customId, spaceId);
			requestFuture.get(timeout, TimeUnit.MILLISECONDS); // wait for object to be received
			
			List<DataObject> storedDataObjects = dataHandler.retrieveDataObjects(spaceId);
			boolean dataObjectFound = false;
			for (DataObject storedObject : storedDataObjects) {
				if (customId.equals(storedObject.getElement().getAttributeValue("customId"))) {
					dataObjectFound = true;
					break;
				}
			}
			assertTrue("The published data object could not be retrieved from local cache.", dataObjectFound);
		} catch (SpaceManagementException e) {
			fail("Failed to update space configuration: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (UnknownEntityException e) {
			fail("Space unknown: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Failed to process request: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Failed to executre request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("The request timed out: " + e.getMessage());
		}
	}
	
	@Ignore
	@Test
	public void testMethodSetMode() {
		// TODO Implement test.
		fail("Test is not implemented.");
	}
	
	@Ignore
	@Test
	public void testMethodGetMode() {
		// TODO Implement test.
		fail("Test is not implemented.");
	}
	
	@Test
	public void testMethodGetDataObjectFilter() {
		DataObjectFilter filter = new NamespaceFilter("mirror:application:ping:ping");
		dataHandler.setDataObjectFilter(filter);
		DataObjectFilter returnedFilter = dataHandler.getDataObjectFilter();
		assertEquals("The filter returned does not equal the filter set.", filter, returnedFilter);
	}
	
	@Test
	public void testMethodSetDataObjectFilter() {
		final String namespace = "mirror:application:ping:ping";
		String spaceId = props.getProperty("test.space.team.id"); // connectionHandler.getCurrentUser().getUsername(); // private space
		String customId = UUID.randomUUID().toString(); 
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		
		Space space = (Space) spaceHandler.getSpace(spaceId);
		SpaceConfiguration spaceConfiguration = space.generateSpaceConfiguration();
		spaceConfiguration.setPersistenceType(PersistenceType.ON);
		
		try {
			
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register test listener. Space: " + spaceId);
		}
		
		final RequestFuture<DataObject> requestFuture = new RequestFuture<DataObject>();
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				assertEquals("The namespace filter was not applied. Received: " + dataObject.getNamespaceURI(), namespace, dataObject.getNamespaceURI());
				requestFuture.setResponse(dataObject);
			}
		};
		
		dataHandler.addDataObjectListener(listener);
		DataObjectFilter filter = new NamespaceFilter(namespace);
		dataHandler.setDataObjectFilter(filter);
		
		DataObject pingObject = generatePingObject(customId);
		try {
			dataHandler.publishDataObject(pingObject, spaceId);
			requestFuture.get(timeout, TimeUnit.MILLISECONDS);
		} catch (UnknownEntityException e) {
			fail("The test space could not be found: " + spaceId);
		} catch (InvalidDataException e) {
			fail("The data object was rejected: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Failed to handle request: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Failed to handle request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("A valid data object was not received.");
		}
		
		DataObject moodObject = generateInvalidMoodObject();
		try {
			dataHandler.publishDataObject(moodObject, spaceId);
		} catch (UnknownEntityException e) {
			fail("The test space could not be found: " + spaceId);
		} catch (InvalidDataException e) {
			fail("The data object was rejected: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodQueryDataObjectById() {
		String spaceId = props.getProperty("test.space.team.id"); // connectionHandler.getCurrentUser().getUsername(); // private space
		String customId = UUID.randomUUID().toString(); 
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		
		Space space = (Space) spaceHandler.getSpace(spaceId);
		SpaceConfiguration spaceConfiguration = space.generateSpaceConfiguration();
		spaceConfiguration.setPersistenceType(PersistenceType.ON);
		
		try {
			spaceHandler.configureSpace(spaceId, spaceConfiguration);
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register test listener. Space: " + spaceId);
		} catch (SpaceManagementException e) {
			fail("Failed to configure space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Invalid connection status: " + e.getMessage());
		}
		
		final RequestFuture<DataObject> requestFuture = new RequestFuture<DataObject>();
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				requestFuture.setResponse(dataObject);
			}
		};
		dataHandler.addDataObjectListener(listener);
		
		try {
			publishPing(customId, spaceId);
			DataObject receivedObject = requestFuture.get(timeout, TimeUnit.MILLISECONDS);
			DataObject queriedObject = dataHandler.queryDataObjectById(receivedObject.getId());
			assertNotNull("No data object was returned.", queriedObject);
			assertEquals("The queried data object differs from the original data object.", receivedObject.getId(), queriedObject.getId());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Failed to process request: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Failed to executre request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("The request timed out: " + e.getMessage());
		} catch (UnsupportedOperationException e) {
			fail("Queries are not supported: " + e.getMessage());
		} catch (QueryException e) {
			fail("The data object query failed: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodQueryDataObjectsById() {
		String spaceId = props.getProperty("test.space.team.id"); // connectionHandler.getCurrentUser().getUsername(); // private space
		String[] customIds = new String[] {UUID.randomUUID().toString(), UUID.randomUUID().toString()};
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		
		Space space = (Space) spaceHandler.getSpace(spaceId);
		SpaceConfiguration spaceConfiguration = space.generateSpaceConfiguration();
		spaceConfiguration.setPersistenceType(PersistenceType.ON);
		
		try {
			spaceHandler.configureSpace(spaceId, spaceConfiguration);
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register test listener. Space: " + spaceId);
		} catch (SpaceManagementException e) {
			fail("Failed to configure space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Invalid connection status: " + e.getMessage());
		}
		
		final Map<String, RequestFuture<DataObject>> requestFutures = new HashMap<String, RequestFuture<DataObject>>();
		requestFutures.put(customIds[0], new RequestFuture<DataObject>());
		requestFutures.put(customIds[1], new RequestFuture<DataObject>());
		
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				String customId = dataObject.getElement().getAttributeValue("customId");
				requestFutures.get(customId).setResponse(dataObject);
			}
		};
		dataHandler.addDataObjectListener(listener);
		
		try {
			publishPing(customIds[0], spaceId);
			DataObject[] receivedObjects = new DataObject[2]; 
			receivedObjects[0] = requestFutures.get(customIds[0]).get(timeout, TimeUnit.MILLISECONDS);
			Thread.sleep(1000l);
			Date publishingDateForSecondObject = new Date();
			publishPing(customIds[1], spaceId);
			receivedObjects[1] = requestFutures.get(customIds[1]).get(timeout, TimeUnit.MILLISECONDS);
			Set<String> objectIds = new HashSet<String>();
			for (DataObject receivedObject : receivedObjects) objectIds.add(receivedObject.getId());

			List<DataObject> queriedObjects = dataHandler.queryDataObjectsById(objectIds, new HashSet<SerializableDataObjectFilter>());
			assertNotNull("A null response was returned.", queriedObjects);
			assertEquals("An invalid number of data objects was returned: " + queriedObjects.size(), 2, queriedObjects.size());
			boolean firstObjectFound = false, secondObjectFound = false;
			for (DataObject queriedObject : queriedObjects) {
					if (queriedObject.getId().equals(receivedObjects[0].getId())) {
						firstObjectFound = true;
					} else {
						secondObjectFound = true;
					}
			}
			assertTrue("Expected data object was not returned: " + receivedObjects[0].getId(), firstObjectFound);
			assertTrue("Expected data object was not returned: " + receivedObjects[1].getId(), secondObjectFound);
			
			Thread.sleep(1000l);
			Set<SerializableDataObjectFilter> filters = new HashSet<SerializableDataObjectFilter>();
			filters.add(new PeriodFilter(publishingDateForSecondObject, new Date()));
			queriedObjects = dataHandler.queryDataObjectsById(objectIds, filters);
			assertNotNull("A null response was returned.", queriedObjects);
			assertEquals("An invalid number of data objects was returned: " + queriedObjects.size(), 1, queriedObjects.size());
			assertEquals("A wrong object was returned: " + queriedObjects.get(0), receivedObjects[1].getId(), queriedObjects.get(0).getId());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Failed to process request: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Failed to executre request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("The request timed out: " + e.getMessage());
		} catch (UnsupportedOperationException e) {
			fail("Queries are not supported: " + e.getMessage());
		} catch (QueryException e) {
			fail("The data object query failed: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodQueryDataObjectsBySpace() {
		String spaceId = props.getProperty("test.space.team.id"); // connectionHandler.getCurrentUser().getUsername(); // private space
		String[] customIds = new String[] {UUID.randomUUID().toString(), UUID.randomUUID().toString()};
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		
		Space space = (Space) spaceHandler.getSpace(spaceId);
		SpaceConfiguration spaceConfiguration = space.generateSpaceConfiguration();
		spaceConfiguration.setPersistenceType(PersistenceType.ON);
		
		try {
			spaceHandler.configureSpace(spaceId, spaceConfiguration);
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register test listener. Space: " + spaceId);
		} catch (SpaceManagementException e) {
			fail("Failed to configure space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Invalid connection status: " + e.getMessage());
		}
		
		final Map<String, RequestFuture<DataObject>> requestFutures = new HashMap<String, RequestFuture<DataObject>>();
		requestFutures.put(customIds[0], new RequestFuture<DataObject>());
		requestFutures.put(customIds[1], new RequestFuture<DataObject>());
		
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				String customId = dataObject.getElement().getAttributeValue("customId");
				requestFutures.get(customId).setResponse(dataObject);
			}
		};
		dataHandler.addDataObjectListener(listener);
		
		try {publishPing(customIds[0], spaceId);
			DataObject[] receivedObjects = new DataObject[2]; 
			receivedObjects[0] = requestFutures.get(customIds[0]).get(timeout, TimeUnit.MILLISECONDS);
			publishPing(customIds[1], spaceId);
			receivedObjects[1] = requestFutures.get(customIds[1]).get(timeout, TimeUnit.MILLISECONDS);
			List<DataObject> queriedObjects = dataHandler.queryDataObjectsBySpace(spaceId, new HashSet<SerializableDataObjectFilter>());
			assertNotNull("A null response was returned.", queriedObjects);
			boolean firstObjectFound = false, secondObjectFound = false;
			for (DataObject queriedObject : queriedObjects) {
					if (queriedObject.getId().equals(receivedObjects[0].getId())) {
						firstObjectFound = true;
					} else {
						secondObjectFound = true;
					}
			}
			assertTrue("Expected data object was not returned: " + receivedObjects[0].getId(), firstObjectFound);
			assertTrue("Expected data object was not returned: " + receivedObjects[1].getId(), secondObjectFound);
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Failed to process request: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Failed to executre request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("The request timed out: " + e.getMessage());
		} catch (UnsupportedOperationException e) {
			fail("Queries are not supported: " + e.getMessage());
		} catch (QueryException e) {
			fail("The data object query failed: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodQueryDataObjectsBySpaces() {
		String[] spaceIds = new String[] {connectionHandler.getCurrentUser().getUsername(), props.getProperty("test.space.team.id")}; 
		
		String[] customIds = new String[] {UUID.randomUUID().toString(), UUID.randomUUID().toString()};
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		
		Space[] spaces = new Space[] {(Space) spaceHandler.getSpace(spaceIds[0]), (Space) spaceHandler.getSpace(spaceIds[1])}; 
		
		try {
			for (Space space : spaces) {
				SpaceConfiguration spaceConfiguration = space.generateSpaceConfiguration();
				spaceConfiguration.setPersistenceType(PersistenceType.ON);
				spaceHandler.configureSpace(space.getId(), spaceConfiguration);
				dataHandler.registerSpace(space.getId());
			}
		} catch (UnknownEntityException e) {
			fail("Failed to register test listener.");
		} catch (SpaceManagementException e) {
			fail("Failed to configure space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Invalid connection status: " + e.getMessage());
		}
		
		final Map<String, RequestFuture<DataObject>> requestFutures = new HashMap<String, RequestFuture<DataObject>>();
		requestFutures.put(customIds[0], new RequestFuture<DataObject>());
		requestFutures.put(customIds[1], new RequestFuture<DataObject>());
		
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				String customId = dataObject.getElement().getAttributeValue("customId");
				requestFutures.get(customId).setResponse(dataObject);
			}
		};
		dataHandler.addDataObjectListener(listener);
		
		try {
			publishPing(customIds[0], spaces[0].getId());
			DataObject[] receivedObjects = new DataObject[2]; 
			receivedObjects[0] = requestFutures.get(customIds[0]).get(timeout, TimeUnit.MILLISECONDS);
			publishPing(customIds[1], spaces[1].getId());
			receivedObjects[1] = requestFutures.get(customIds[1]).get(timeout, TimeUnit.MILLISECONDS);
			List<DataObject> queriedObjects = dataHandler.queryDataObjectsBySpaces(new HashSet<String>(Arrays.asList(spaceIds)), new HashSet<SerializableDataObjectFilter>());
			assertNotNull("A null response was returned.", queriedObjects);
			boolean firstObjectFound = false, secondObjectFound = false;
			for (DataObject queriedObject : queriedObjects) {
					if (queriedObject.getId().equals(receivedObjects[0].getId())) {
						firstObjectFound = true;
					} else {
						secondObjectFound = true;
					}
			}
			assertTrue("Expected data object was not returned: " + receivedObjects[0].getId(), firstObjectFound);
			assertTrue("Expected data object was not returned: " + receivedObjects[1].getId(), secondObjectFound);
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Failed to process request: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Failed to executre request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("The request timed out: " + e.getMessage());
		} catch (UnsupportedOperationException e) {
			fail("Queries are not supported: " + e.getMessage());
		} catch (QueryException e) {
			fail("The data object query failed: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodDeleteDataObject() {
		String spaceId = connectionHandler.getCurrentUser().getUsername(); // private space
		String customId = UUID.randomUUID().toString(); 
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		
		Space space = (Space) spaceHandler.getSpace(spaceId);
		SpaceConfiguration spaceConfiguration = space.generateSpaceConfiguration();
		spaceConfiguration.setPersistenceType(PersistenceType.ON);
		
		try {
			spaceHandler.configureSpace(spaceId, spaceConfiguration);
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register test listener. Space: " + spaceId);
		} catch (SpaceManagementException e) {
			fail("Failed to configure space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Invalid connection status: " + e.getMessage());
		}
		
		final RequestFuture<DataObject> requestFuture = new RequestFuture<DataObject>();
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				requestFuture.setResponse(dataObject);
			}
		};
		dataHandler.addDataObjectListener(listener);
		
		try {
			publishPing(customId, spaceId);
			DataObject receivedObject = requestFuture.get(timeout, TimeUnit.MILLISECONDS);
			DataObject queriedObject = dataHandler.queryDataObjectById(receivedObject.getId());
			assertNotNull("No data object was returned after creation.", queriedObject);
			boolean isIndicatedAsDeleted = dataHandler.deleteDataObject(receivedObject.getId());
			assertTrue("The deletion was not confirmed.", isIndicatedAsDeleted);
			if (dataHandler.queryDataObjectById(receivedObject.getId()) != null) {
				fail("The deleted data object can still be retrieved.");
			}
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Failed to process request: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Failed to executre request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("The request timed out: " + e.getMessage());
		} catch (UnsupportedOperationException e) {
			fail("Queries are not supported: " + e.getMessage());
		} catch (QueryException e) {
			fail("The data object query failed: " + e.getMessage());
		}
	}
	
	@Test
	public void testMethodDeleteDataObjects() {
		String spaceId = props.getProperty("test.space.team.id"); // connectionHandler.getCurrentUser().getUsername(); // private space
		String[] customIds = new String[] {UUID.randomUUID().toString(), UUID.randomUUID().toString()};
		int timeout = Integer.parseInt(props.getProperty("test.params.timeout"));
		
		Space space = (Space) spaceHandler.getSpace(spaceId);
		SpaceConfiguration spaceConfiguration = space.generateSpaceConfiguration();
		spaceConfiguration.setPersistenceType(PersistenceType.ON);
		
		try {
			spaceHandler.configureSpace(spaceId, spaceConfiguration);
			dataHandler.registerSpace(spaceId);
		} catch (UnknownEntityException e) {
			fail("Failed to register test listener. Space: " + spaceId);
		} catch (SpaceManagementException e) {
			fail("Failed to configure space: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Invalid connection status: " + e.getMessage());
		}
		
		final Map<String, RequestFuture<DataObject>> requestFutures = new HashMap<String, RequestFuture<DataObject>>();
		requestFutures.put(customIds[0], new RequestFuture<DataObject>());
		requestFutures.put(customIds[1], new RequestFuture<DataObject>());
		
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				String customId = dataObject.getElement().getAttributeValue("customId");
				requestFutures.get(customId).setResponse(dataObject);
			}
		};
		dataHandler.addDataObjectListener(listener);
		
		try {
			publishPing(customIds[0], spaceId);
			DataObject[] receivedObjects = new DataObject[2]; 
			receivedObjects[0] = requestFutures.get(customIds[0]).get(timeout, TimeUnit.MILLISECONDS);
			publishPing(customIds[1], spaceId);
			receivedObjects[1] = requestFutures.get(customIds[1]).get(timeout, TimeUnit.MILLISECONDS);
			Set<String> objectIds = new HashSet<String>();
			for (DataObject receivedObject : receivedObjects) objectIds.add(receivedObject.getId());

			List<DataObject> queriedObjects = dataHandler.queryDataObjectsById(objectIds, new HashSet<SerializableDataObjectFilter>());
			assertEquals("Failed to create data objects for testing.", 2, queriedObjects.size());
			int indicatedNumberOfDeletedObjects = dataHandler.deleteDataObjects(objectIds);
			assertEquals("Invalid number of data objects was indicated to be deleted.", 2, indicatedNumberOfDeletedObjects);
			if (dataHandler.queryDataObjectsById(objectIds, new HashSet<SerializableDataObjectFilter>()).size() > 0) {
				fail("Not all data objects were deleted.");
			}
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Failed to process request: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Failed to executre request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("The request timed out: " + e.getMessage());
		} catch (UnsupportedOperationException e) {
			fail("Queries are not supported: " + e.getMessage());
		} catch (QueryException e) {
			fail("The data object query failed: " + e.getMessage());
		}
	}
	
	/*
	@Test
	public void testReceivingDataFromPrivateSpace() {
		final RequestFuture<DataObject> requestFuture = new RequestFuture<DataObject>();
		final DataObject ping = generatePingObject();
		DataObjectListener listener = new DataObjectListener() {
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				requestFuture.setResponse(dataObject);
			}
		};
		
		try {
			dataHandler.addDataObjectListener(listener);
			dataHandler.registerSpace(privateSpace.getId());
		} catch (UnknownEntityException e) {
			fail("Failed to register for space " + privateSpace.getId() + ": " + e.getMessage());
		}
		
		try {
			dataHandler.publishDataObject(ping, privateSpace.getId());
			DataObject receivedObject = requestFuture.get(2, TimeUnit.SECONDS);
			assertEquals("Received data object does not equal ", ((CDMData_1_0) ping.getCDMData()).getCustomId(), ((CDMData_1_0) receivedObject.getCDMData()).getCustomId());
		} catch (UnknownEntityException e) {
			fail("Failed to publish on private space: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("Request was interrupted: " + e.getMessage());
		} catch (ExecutionException e) {
			fail("Failed to execute request: " + e.getMessage());
		} catch (TimeoutException e) {
			fail("Timed out during publishing: " + e.getMessage());
		}
	}
	*/
}
