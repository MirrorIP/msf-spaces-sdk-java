package de.imc.mirror.sdk.java;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.DataObjectListener;
import de.imc.mirror.sdk.OfflineModeHandler.Mode;
import de.imc.mirror.sdk.SerializableDataObjectFilter;
import de.imc.mirror.sdk.Space.PersistenceType;
import de.imc.mirror.sdk.Space.Type;
import de.imc.mirror.sdk.SpaceMember.Role;
import de.imc.mirror.sdk.cdm.CDMVersion;
import de.imc.mirror.sdk.exceptions.ConnectionStatusException;
import de.imc.mirror.sdk.exceptions.InvalidDataException;
import de.imc.mirror.sdk.exceptions.QueryException;
import de.imc.mirror.sdk.exceptions.SpaceManagementException;
import de.imc.mirror.sdk.exceptions.UnknownEntityException;
import de.imc.mirror.sdk.filter.NamespaceFilter.CompareType;
import de.imc.mirror.sdk.java.data.DatabaseConfig;
import de.imc.mirror.sdk.java.exceptions.InvalidBuildException;
import de.imc.mirror.sdk.java.filter.DataModelFilter;
import de.imc.mirror.sdk.java.filter.NamespaceFilter;
import de.imc.mirror.sdk.java.filter.PeriodFilter;
import de.imc.mirror.sdk.java.filter.PublisherFilter;
import de.imc.mirror.sdk.java.filter.ReferencesFilter;

public class DataObjectFilterTest {
	private static final String PROPERTIES_FILE = "test/connection.properties";
	
	private Properties props;
	private ConnectionHandler connectionHandler;
	private SpaceHandler spaceHandler;
	private Space testSpace;
	private DataHandler dataHandler;
	private DataObject testPingObject, testMoodObject;
	
	@Before
	public void initialize() throws FileNotFoundException, IOException, ConnectionStatusException, SpaceManagementException, UnknownEntityException, InvalidDataException, InterruptedException, ExecutionException, TimeoutException {
		props = new Properties();
		props.load(new FileInputStream(PROPERTIES_FILE));
		ConnectionConfigurationBuilder configBuilder = new ConnectionConfigurationBuilder(props.getProperty("xmpp.domain"), props.getProperty("xmpp.appid"));
		ConnectionConfiguration connectionConfig = configBuilder.build();
		connectionHandler = new ConnectionHandler(props.getProperty("test.user.01.id"), props.getProperty("test.user.01.password"), connectionConfig);
		connectionHandler.connect();
		spaceHandler = new SpaceHandler(connectionHandler, DatabaseConfig.disableDatabaseCaching());
		spaceHandler.setMode(Mode.ONLINE);
		
		Set<de.imc.mirror.sdk.SpaceMember> spaceMembers = new HashSet<de.imc.mirror.sdk.SpaceMember>();
		spaceMembers.add(new SpaceMember(props.getProperty("test.user.01.id") + "@" + connectionConfig.getDomain(), Role.MODERATOR));
		spaceMembers.add(new SpaceMember(props.getProperty("test.user.02.id") + "@" + connectionConfig.getDomain(), Role.MEMBER));
		SpaceConfiguration spaceConfig = new SpaceConfiguration(Type.TEAM, "[TESTING] Filtering", spaceMembers, PersistenceType.ON, null);
		testSpace = (Space) spaceHandler.createSpace(spaceConfig);
		
		dataHandler = new DataHandler(connectionHandler, spaceHandler);
		dataHandler.setMode(Mode.ONLINE);
		dataHandler.registerSpace(testSpace.getId());
		final String[] customIds = new String[] {UUID.randomUUID().toString(), UUID.randomUUID().toString()};
		final RequestFuture<DataObject> pingObjectFuture = new RequestFuture<DataObject>();
		final RequestFuture<DataObject> moodObjectFuture = new RequestFuture<DataObject>();

		dataHandler.addDataObjectListener(new DataObjectListener() {
			
			@Override
			public void handleDataObject(DataObject dataObject, String spaceId) {
				String customId = dataObject.getElement().getAttributeValue("customId");
				if (customIds[0].equals(customId)) {
					pingObjectFuture.setResponse(dataObject);					
				} else if (customIds[1].equals(customId)) {
					moodObjectFuture.setResponse(dataObject);
				}
			}
		});

		dataHandler.publishDataObject(generatePingObject(customIds[0]), testSpace.getId());
		
		testPingObject = pingObjectFuture.get(2, TimeUnit.SECONDS);
		System.out.println("DataObjectFilterTest: Created ping object with id: " + testPingObject.getId());
		Thread.sleep(2000l);
		dataHandler.publishDataObject(generateMoodObject(customIds[1], testPingObject.getId()), testSpace.getId());
		testMoodObject = moodObjectFuture.get(2, TimeUnit.SECONDS);
		System.out.println("DataObjectFilterTest: Created mood object with id: " + testMoodObject.getId());
	}
	
	@After
	public void destroy() throws SpaceManagementException, ConnectionStatusException {
		spaceHandler.deleteSpace(testSpace.getId());
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
	
	private DataObject generateMoodObject(String customId, String ref) {
		CDMDataBuilder cdmBuilder = new CDMDataBuilder(CDMVersion.CDM_1_0);
		cdmBuilder.setPublisher(connectionHandler.getCurrentUser().getFullJID());
		cdmBuilder.setModelVersion("1.0");
		cdmBuilder.setCustomId(customId);
		cdmBuilder.setRef(ref);
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
	
	
	@Test
	public void testDataModelFilter() {
		Set<SerializableDataObjectFilter> filterSet = new HashSet<SerializableDataObjectFilter>();
		try {
			filterSet.add(new DataModelFilter("mirror:application:ping:ping", "1.0"));
			List<DataObject> dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testPingObject.getId(), dataObjects.get(0).getId());
			
			filterSet.clear();
			filterSet.add(new DataModelFilter("mirror:application:integratedmoodmap:mood"));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testMoodObject.getId(), dataObjects.get(0).getId());
			
			filterSet.clear();
			filterSet.add(new DataModelFilter("mirror:application:ping:ping", "0.1"));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 0, dataObjects.size());
		} catch (UnsupportedOperationException e) {
			fail("Failed to perform a query: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (QueryException e) {
			fail("Invalid query: " + e.getMessage());
		}
	}
	
	@Test
	public void testNamespaceFilter() {
		Set<SerializableDataObjectFilter> filterSet = new HashSet<SerializableDataObjectFilter>();
		List<DataObject> dataObjects;
		try {
			filterSet.add(new NamespaceFilter("mirror:application:ping:ping"));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testPingObject.getId(), dataObjects.get(0).getId()); 
			
			filterSet.clear();
			filterSet.add(new NamespaceFilter(CompareType.STRICT, "mirror:application:integratedmoodmap:mood"));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testMoodObject.getId(), dataObjects.get(0).getId()); 
			
			filterSet.clear();
			filterSet.add(new NamespaceFilter(CompareType.CONTAINS, "mood"));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testMoodObject.getId(), dataObjects.get(0).getId()); 
			
			filterSet.clear();
			filterSet.add(new NamespaceFilter(CompareType.REGEX, ".*m..d.*"));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testMoodObject.getId(), dataObjects.get(0).getId());
		} catch (UnsupportedOperationException e) {
			fail("Failed to perform a query: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (QueryException e) {
			fail("Invalid query: " + e.getMessage());
		}
	}
	
	@Test
	public void testPeriodFilter() {
		Set<SerializableDataObjectFilter> filterSet = new HashSet<SerializableDataObjectFilter>();
		List<DataObject> dataObjects;
		Date moodPublishingDate = testMoodObject.getCDMData().getTimeStamp();
		try {
			filterSet.add(new PeriodFilter(new Date(moodPublishingDate.getTime() - 500), new Date(moodPublishingDate.getTime() + 500)));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testMoodObject.getId(), dataObjects.get(0).getId());
			
			filterSet.clear();
			filterSet.add(new PeriodFilter(null, new Date(moodPublishingDate.getTime() - 500)));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testPingObject.getId(), dataObjects.get(0).getId());

			filterSet.clear();
			filterSet.add(new PeriodFilter(new Date(moodPublishingDate.getTime() - 500), null));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testMoodObject.getId(), dataObjects.get(0).getId());
		} catch (UnsupportedOperationException e) {
			fail("Failed to perform a query: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (QueryException e) {
			fail("Invalid query: " + e.getMessage());
		}
	}
	
	@Test
	public void testPublisherFilter() {
		Set<SerializableDataObjectFilter> filterSet = new HashSet<SerializableDataObjectFilter>();
		List<DataObject> dataObjects;
		try {
			filterSet.add(new PublisherFilter(props.getProperty("test.user.01.id") + "@" + connectionHandler.getConfiguration().getDomain()));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testMoodObject.getId(), dataObjects.get(0).getId());
			
			filterSet.clear();
			filterSet.add(new PublisherFilter(props.getProperty("test.user.02.id") + "@" + connectionHandler.getConfiguration().getDomain()));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertTrue("No data objects should be returned.", dataObjects.isEmpty());
		} catch (UnsupportedOperationException e) {
			fail("Failed to perform a query: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (QueryException e) {
			fail("Invalid query: " + e.getMessage());
		}
	}
	
	@Test
	public void testReferenceFilter() {
		Set<SerializableDataObjectFilter> filterSet = new HashSet<SerializableDataObjectFilter>();
		List<DataObject> dataObjects;
		try {
			filterSet.add(new ReferencesFilter(testPingObject.getId()));
			dataObjects = dataHandler.queryDataObjectsBySpace(testSpace.getId(), filterSet);
			assertEquals("Wrong number of data object retrieved.", 1, dataObjects.size());
			assertEquals("Wrong data object retrieved.", testMoodObject.getId(), dataObjects.get(0).getId());
		} catch (UnsupportedOperationException e) {
			fail("Failed to perform a query: " + e.getMessage());
		} catch (ConnectionStatusException e) {
			fail("Wrong connection status: " + e.getMessage());
		} catch (QueryException e) {
			fail("Invalid query: " + e.getMessage());
		}
	}
}
