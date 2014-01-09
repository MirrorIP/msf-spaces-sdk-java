package de.imc.mirror.sdk.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.imc.mirror.sdk.DataModel;
import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.Space;
import de.imc.mirror.sdk.SpaceChannel;
import de.imc.mirror.sdk.SpaceMember;
import de.imc.mirror.sdk.Space.PersistenceType;
import de.imc.mirror.sdk.SpaceMember.Role;
import de.imc.mirror.sdk.java.data.ChannelsTable;
import de.imc.mirror.sdk.java.data.DataTable;
import de.imc.mirror.sdk.java.data.DatabaseConfig;
import de.imc.mirror.sdk.java.data.MembersTable;
import de.imc.mirror.sdk.java.data.SendTable;
import de.imc.mirror.sdk.java.data.SpacesTable;
import de.imc.mirror.sdk.java.data.DatabaseConfig.Type;

import java.io.IOException;
import java.io.StringReader;
import java.sql.*;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jivesoftware.smackx.pubsub.SimplePayload;

/**
 * Wrapper to access an external database database.
 * Used internally by both space and data handler.
 * 
 * {@link DataHandler}
 * {@link SpaceHandler}
 * @author mach
 *
 */
public class DataWrapperExtern extends DataWrapper {

	private static Connection db;
	private static Type type;
	private static Logger logger;
	
	/**
	 * Create a new DataWrapper.
	 */
	protected DataWrapperExtern(DatabaseConfig config) {
		logger = Logger.getAnonymousLogger();
		try {
			Runtime.getRuntime().removeShutdownHook(ShutdownInterceptor.getInstance());
			Runtime.getRuntime().addShutdownHook(ShutdownInterceptor.getInstance());
			Class.forName(config.getDriverLocation());
			type = config.getType();
			db = DriverManager.getConnection(config.getConnectionURL(), 
											 config.getDbUser(), 
											 config.getDbPassword());
			db.createStatement().execute(SpacesTable.SQL_CREATE);
			db.createStatement().execute(ChannelsTable.SQL_CREATE);
			db.createStatement().execute(MembersTable.SQL_CREATE);
			db.createStatement().execute(SendTable.SQL_CREATE);
			db.createStatement().execute(DataTable.SQL_CREATE);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "An Exception occured while establishing a connection to the database", e);
		} catch (ClassNotFoundException e) {
			logger.log(Level.SEVERE, "An Exception occured while establishing a connection to the database", e);
		}
	}
	
	protected static void shutdown()
	{
		if (DatabaseConfig.Type.EMBEDDED == type){
			try
			{
				Statement s = db.createStatement();
				s.executeUpdate( "SHUTDOWN" );
				s.close();
				db.close();
			}
			catch( Exception e )
			{
				logger.log(Level.SEVERE, "An Exception occured while shutting down the embedded database", e);
			}
		}
	}
	
	@Override
	protected void deleteCachedSpacesForUser(String user){
		List<String> spaceIds = getSpaceIdsForUser(user);
		if (spaceIds.size() > 0){
			List<String> spaceIdsOfOthers = getSpacesAlsoSavedForOthers(spaceIds, user);
			spaceIds.removeAll(spaceIdsOfOthers);
		}
		try {
			Statement s = db.createStatement();
			s.execute("DELETE FROM " + SpacesTable.TABLE_NAME + " WHERE " + SpacesTable.TABLE_NAME + "." + SpacesTable.USER + " ='" + user + "';");
			for (String spaceId:spaceIds){
				s.execute("DELETE FROM " + ChannelsTable.TABLE_NAME + " WHERE " + ChannelsTable.TABLE_NAME + "." + ChannelsTable.SPACE + " ='" + spaceId + "';");
				s.execute("DELETE FROM " + MembersTable.TABLE_NAME + " WHERE " + MembersTable.TABLE_NAME + "." + MembersTable.SPACE + " ='" + spaceId + "';");
			}
			s.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "An Exception occured while deleting the cached spacesinformation for a user", e);
		}
	}
	
	@Override
	protected void deleteCachedSpace(String spaceId){
		try {
			Statement s = db.createStatement();
			s.execute("DELETE FROM " + SpacesTable.TABLE_NAME + " WHERE " + SpacesTable.TABLE_NAME + "." + SpacesTable.SPACE_ID + " ='" + spaceId + "';");
			s.execute("DELETE FROM " + ChannelsTable.TABLE_NAME + " WHERE " + ChannelsTable.TABLE_NAME + "." + ChannelsTable.SPACE + " ='" + spaceId + "';");
			s.execute("DELETE FROM " + MembersTable.TABLE_NAME + " WHERE " + MembersTable.TABLE_NAME + "." + MembersTable.SPACE + " ='" + spaceId + "';");
			s.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "An Exception occured while deleting a cached space", e);
		}
	}
	
	/**
	 * Gets all ids of spaces which are also cached for other users.
	 * @param spaceIds The spaceIds to check.
	 * @param user The user to exclude from the search.
	 * @return A list of all ids of the spaces which are also cached for other users.
	 */
	private List<String> getSpacesAlsoSavedForOthers(List<String> spaceIds, String user){
		StringBuilder builder = new StringBuilder();
		for (int i=0; i<spaceIds.size();i++){
			String spaceId = spaceIds.get(i);
			builder.append(SpacesTable.TABLE_NAME + "." + SpacesTable.SPACE_ID + "='");
			builder.append(spaceId);
			builder.append("' ");
			if (i != spaceIds.size()-1){
				builder.append("OR ");
			}
		}
		String query = "SELECT " + SpacesTable.TABLE_NAME + "." + SpacesTable.SPACE_ID + " FROM " + SpacesTable.TABLE_NAME 
						+ " WHERE NOT(" + SpacesTable.TABLE_NAME + "." + SpacesTable.USER + "='" + user + "') AND (" + 
						builder.toString() + ");";
		List<String> result = new ArrayList<String>();
		try {
			Statement idsStatement = db.createStatement();
			ResultSet rs = idsStatement.executeQuery(query);
			while(rs.next()){
				String spaceId = rs.getString(SpacesTable.SPACE_ID);
				result.add(spaceId);
			}
			idsStatement.close();
			rs.close();
		} catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while retrieving information from the database", e);
		}
		return result;
	}
	
	@Override
	protected List<String> getSpaceIdsForUser(String user){
		try{
		Statement s = db.createStatement();
		String query = "SELECT * FROM " + SpacesTable.TABLE_NAME + " WHERE " + SpacesTable.TABLE_NAME + "." + SpacesTable.USER + "='" + user + "'";
		ResultSet rs = s.executeQuery(query);
		List<String> spaceIds = new ArrayList<String>();
		while (rs.next()){
			String spaceId = rs.getString(SpacesTable.SPACE_ID);
			spaceIds.add(spaceId);
		}
		rs.close();
		s.close();
		return spaceIds;
		}catch(Exception e){
			logger.log(Level.SEVERE, "An Exception occured while retrieving information from the database", e);
		}
		return null;
	}

	@Override
	protected void saveSpace(Space space, String user){
		try{
			PreparedStatement spacesStatement = db.prepareStatement("insert into " + SpacesTable.TABLE_NAME + " values (?,?,?,?,?,?)");
			spacesStatement.setString(1, space.getId());
			spacesStatement.setString(2, space.getName());
			spacesStatement.setString(3, space.getDomain());
			spacesStatement.setString(4, space.getType().toString());
			String persistentString;
			switch (space.getPersistenceType()) {
			case ON:
				persistentString = "true";
				break;
			case DURATION:
				persistentString = space.getPersistenceDuration().toString();
				break;
			default:
				persistentString = "false";
			}
			spacesStatement.setString(5, persistentString);
			spacesStatement.setString(6, user);
			spacesStatement.executeUpdate();
			spacesStatement.close();
			saveSpaceChannelsAndMembers(space);
		}catch (Exception e){
			logger.log(Level.SEVERE, "An Exception occured while saving information in the database", e);
		}	
	}

	@Override
	protected List<Space> getCachedSpacesForUser(String user){
		String query = "Select * FROM " + SpacesTable.TABLE_NAME + " WHERE " +
		 		SpacesTable.TABLE_NAME + "." + SpacesTable.USER + "='" + user + "';";
		List<Space> spaces = new ArrayList<Space>();
		try{
			Statement statement = db.createStatement();
			ResultSet spacesSet = statement.executeQuery(query);
			while (spacesSet.next()){
				String spaceId = spacesSet.getString(SpacesTable.SPACE_ID);
				String membersQuery = "SELECT * FROM " + MembersTable.TABLE_NAME + " WHERE " +
						MembersTable.TABLE_NAME + "." + MembersTable.SPACE + "='" + spaceId + "';";
				ResultSet membersSet = statement.executeQuery(membersQuery);
				Set<SpaceMember> members = new HashSet<SpaceMember>();
				while (membersSet.next()){
					String jid = membersSet.getString(MembersTable.BAREJID);
					String role = membersSet.getString(MembersTable.ROLE);
					members.add(new de.imc.mirror.sdk.java.SpaceMember(jid, Role.valueOf(role)));
				}
				membersSet.close();
				String channelsTypeQuery = "SELECT DISTINCT " + ChannelsTable.TABLE_NAME + "." + ChannelsTable.TYPE + 
											" FROM " + ChannelsTable.TABLE_NAME + " WHERE " +
											ChannelsTable.TABLE_NAME + "." + ChannelsTable.SPACE + "='" + spaceId + "';";
				Set<SpaceChannel> channels = new HashSet<SpaceChannel>();
				ResultSet channelsTypeSet = statement.executeQuery(channelsTypeQuery);
				while (channelsTypeSet.next()){
					String type = channelsTypeSet.getString(ChannelsTable.TYPE);
					String channelsQuery = "SELECT * FROM " + ChannelsTable.TABLE_NAME + " WHERE " +
												ChannelsTable.TABLE_NAME + "." + ChannelsTable.SPACE + "='" + spaceId + "' AND " +
												ChannelsTable.TABLE_NAME + "." + ChannelsTable.TYPE + "='" + type + "';";
					Map<String, String> properties = new HashMap<String,String>();
					ResultSet channelsSet = statement.executeQuery(channelsQuery);
					while (channelsSet.next()){
						String key = channelsSet.getString(ChannelsTable.KEY);
						String value = channelsSet.getString(ChannelsTable.VALUE);
						properties.put(key, value);
					}
					channelsSet.close();
					channels.add(new de.imc.mirror.sdk.java.SpaceChannel(type, properties));
				}
				channelsTypeSet.close();
				String name = spacesSet.getString(SpacesTable.SPACE_NAME);
				String domain = spacesSet.getString(SpacesTable.SPACE_DOMAIN);
				Space.Type type = Space.Type.getType(spacesSet.getString(SpacesTable.SPACE_TYPE));
				String persistentString = spacesSet.getString(SpacesTable.SPACE_PERSISTENT);
				PersistenceType persistenceType;
				Duration persistenceDuration;
				if ("true".equalsIgnoreCase(persistentString)) {
					persistenceType = PersistenceType.ON;
					persistenceDuration = null;
				} else if ("false".equalsIgnoreCase(persistentString)) {
					persistenceType = PersistenceType.OFF;
					persistenceDuration = null;
				} else {
					try {
						persistenceDuration = DatatypeFactory.newInstance().newDuration(persistentString);
						persistenceType = PersistenceType.DURATION;
					} catch (Exception e) {
						// default to "persistence off"
						persistenceType = PersistenceType.OFF;
						persistenceDuration = null;
					}
				}
				spaces.add(de.imc.mirror.sdk.java.Space.createSpace(name, spaceId, domain, null, type, channels, members, persistenceType, persistenceDuration));				
			}
			spacesSet.close();
		}catch(SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while retrieving information from the database", e);
		}
		return spaces;
	}

	@Override
	protected void savePayloadToSend(String user, String payloadId, String spaceId, SimplePayload payload){
		try{
			PreparedStatement sendStatement = db.prepareStatement("insert into " + SendTable.TABLE_NAME + " values (?,?,?,?,?,?)");
			sendStatement.setString(1, payloadId);
			sendStatement.setString(2, spaceId);
			sendStatement.setString(3, payload.getElementName());
			sendStatement.setString(4, payload.getNamespace());
			sendStatement.setString(5, payload.toXML());
			sendStatement.setString(6, user);
			sendStatement.executeUpdate();
			sendStatement.close();
		}catch(SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while saving information in the database", e);
		}
	}
	
	@Override
	protected Map<String, SimplePayload> getPayloadsToSend(String user){
		String query = "Select * FROM " + SendTable.TABLE_NAME + " WHERE " 
						+ SendTable.TABLE_NAME +  "." +	SendTable.USER + "='" + user + "';";
		Map<String, SimplePayload> payloads = new HashMap<String, SimplePayload>();
		try{
			Statement payloadStatement = db.createStatement();
			ResultSet rs = payloadStatement.executeQuery(query);
			while (rs.next()){
				String name = rs.getString(SendTable.SEND_NAME);
				String namespace = rs.getString(SendTable.SEND_NAMESPACE);
				String payload = rs.getString(SendTable.SEND_PAYLOAD);
				String id = rs.getString(SendTable.SEND_ID);
				SimplePayload simplePayload = new SimplePayload(name, namespace, payload);
				payloads.put(id, simplePayload);
			}
			payloadStatement.close();
		}catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while retrieving information from the database", e);
		}
		return payloads;
	}

	@Override
	protected String getSpaceForPayload(String id){
		String result = null;
		String query = "Select " + SendTable.TABLE_NAME + "." + SendTable.SEND_SPACE + " FROM " + 
								SendTable.TABLE_NAME + " WHERE " + SendTable.TABLE_NAME + "." +
								SendTable.SEND_ID + "='" + id + "';";
		try{
			Statement spaceStatement = db.createStatement();
			ResultSet rs = spaceStatement.executeQuery(query);
			if (rs.next()){
				result = rs.getString(SendTable.SEND_SPACE);				
			}
			spaceStatement.close();
			rs.close();
		} catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while retrieving information from the database", e);
		}
		return result;
	}
	
	@Override
	protected void clearSendCache(String user){
		try{
			Statement deleteStatement = db.createStatement();
			deleteStatement.execute("DELETE FROM " + SendTable.TABLE_NAME + " WHERE " + SendTable.TABLE_NAME + "." +SendTable.USER + " ='" + user +"';");
			deleteStatement.close();
		} catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while deleting information from the database", e);
		}
	}

	@Override
	protected boolean isDataObjectAlreadyCached(String id){
		String query = "Select * FROM " + DataTable.TABLE_NAME + " WHERE " +
						DataTable.TABLE_NAME + "." + DataTable.DATA_ID + "='" + id + "';";
		try{
			Statement itemStatement = db.createStatement();
			ResultSet rs = itemStatement.executeQuery(query);
			if (rs.next()){
				rs.close();
				itemStatement.close();
				return true;
			}
			rs.close();
			itemStatement.close();
		} catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while retrieving information from the database", e);
		}
		return false;
	}

	@Override
	protected void saveDataObject(String nodeId, DataObject obj, String id){
		try{
			PreparedStatement itemStatement = db.prepareStatement("INSERT INTO " + DataTable.TABLE_NAME + 
														" VALUES(?, ?, ?, ?, ?);");
			itemStatement.setString(1, id);
			itemStatement.setString(2, nodeId);
			itemStatement.setString(3, obj.getElement().getName());
			itemStatement.setString(4, obj.getNamespaceURI());
			itemStatement.setString(5, obj.toString());
			itemStatement.executeUpdate();
			itemStatement.close();
		} catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while saving information in the database", e);
		}
	}
	
	@Override
	protected List<DataObject>  getCachedDataObjects(String nodeId){
		String query = "Select * FROM " + DataTable.TABLE_NAME + " WHERE " +
					DataTable.TABLE_NAME + "." + DataTable.DATA_NODE + "='" + nodeId + "';";
		List<DataObject> objs = new ArrayList<DataObject>();
		try{
			Statement objsStatement = db.createStatement();
			ResultSet rs = objsStatement.executeQuery(query);
			while (rs.next()){
				String namespace = rs.getString(DataTable.DATA_NAMESPACE);
				String payload = rs.getString(DataTable.DATA_PAYLOAD);
				
				SAXBuilder reader = new SAXBuilder();
				StringReader in = new StringReader(payload);
				Document document = null;
				try {
				document = reader.build(in);
				} catch (JDOMException e) {
					logger.log(Level.SEVERE, "An JDOMException was thrown while parsing a newly gotten item.", e);
				} catch (IOException e) {
					logger.log(Level.SEVERE, "An IOException was thrown while parsing a newly gotten item.", e);
				}
				if (document == null){
				}
				Element elem = document.getRootElement();
				DataObject obj = new DataObjectBuilder(elem, namespace).build();
		
				objs.add(obj);
			}
			objsStatement.close();
			rs.close();
		} catch(SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while retrieving information from the database", e);
		}
		return objs;
	}

	@Override
	protected void saveSpaces(List<Space> spaces, String user){
		try{
			PreparedStatement spacesStatement = db.prepareStatement("insert into " + SpacesTable.TABLE_NAME + " values (?,?,?,?,?,?)");
			for (Space space:spaces){
				spacesStatement.clearParameters();
				spacesStatement.setString(1, space.getId());
				spacesStatement.setString(2, space.getName());
				spacesStatement.setString(3, space.getDomain());
				spacesStatement.setString(4, space.getType().toString());
				String persistentString;
				switch (space.getPersistenceType()) {
				case ON:
					persistentString = "true";
					break;
				case DURATION:
					persistentString = space.getPersistenceDuration().toString();
					break;
				default:
					persistentString = "false";
				}
				spacesStatement.setString(5, persistentString);
				spacesStatement.setString(6, user);
				spacesStatement.executeUpdate();
				saveSpaceChannelsAndMembers(space);
			}
			spacesStatement.close();
		}catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while saving information in the database", e);
		}
	}

	@Override
	protected void updateCachedSpaceInformation(Space space){
		String spaceId = space.getId();
		String persistentString;
		switch (space.getPersistenceType()) {
		case ON:
			persistentString = "true";
			break;
		case DURATION:
			persistentString = space.getPersistenceDuration().toString();
			break;
		default:
			persistentString = "false";
		}
		try {
			Statement statement = db.createStatement();
			statement.executeUpdate("UPDATE " + SpacesTable.TABLE_NAME + " SET " + 
					SpacesTable.TABLE_NAME + "." + SpacesTable.SPACE_DOMAIN + "='" + space.getDomain()+ "', " +
					SpacesTable.TABLE_NAME + "." + SpacesTable.SPACE_NAME + "='" + space.getName()+ "', " +
					SpacesTable.TABLE_NAME + "." + SpacesTable.SPACE_PERSISTENT + "='" + persistentString + "', " +
					SpacesTable.TABLE_NAME + "." + SpacesTable.SPACE_TYPE + "='" + space.getType().name()+ "' " +
					" WHERE " + SpacesTable.TABLE_NAME + "." + SpacesTable.SPACE_ID + " ='" + spaceId + "';");
			statement.executeUpdate("DELETE FROM " + ChannelsTable.TABLE_NAME + " WHERE " + ChannelsTable.TABLE_NAME + "." + ChannelsTable.SPACE + " ='" + spaceId + "';");
			statement.executeUpdate("DELETE FROM " + MembersTable.TABLE_NAME + " WHERE " + MembersTable.TABLE_NAME + "." + MembersTable.SPACE + " ='" + spaceId + "';");
			statement.close();
			saveSpaceChannelsAndMembers(space);
		} catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while updating information in the database", e);
		}
	}

	@Override
	protected boolean isSpaceAlreadyCached(String spaceId){
		String query = "Select * FROM " + SpacesTable.TABLE_NAME + " WHERE " +
						SpacesTable.TABLE_NAME + "." + SpacesTable.SPACE_ID + "='" + spaceId + "';";
		try{
			Statement spaceStatement = db.createStatement();
			ResultSet rs = spaceStatement.executeQuery(query);
			if (rs.next()){
				spaceStatement.close();
				rs.close();
				return true;
			}
			spaceStatement.close();
			rs.close();
		} catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while retrieving information from the database", e);
		}
		return false;
	}
	
	/**
	 * Convenience method to save the channels and membersn of a space.
	 * @param space The space to save the information for.
	 */
	private void saveSpaceChannelsAndMembers(Space space){
		try{
			PreparedStatement channelsStatement = db.prepareStatement("insert into " + ChannelsTable.TABLE_NAME + " values (?,?,?,?)");
			PreparedStatement membersStatement = db.prepareStatement("insert into " + MembersTable.TABLE_NAME + " values (?, ?, ?)");
			Set<SpaceChannel> channels = space.getChannels();
			for (SpaceChannel channel:channels){
				for (String property:channel.getProperties().keySet()){
					channelsStatement.clearParameters();
					channelsStatement.setString(1, space.getId());
					channelsStatement.setString(2, channel.getType());
					channelsStatement.setString(3, property);
					channelsStatement.setString(4, channel.getProperties().get(property));
					channelsStatement.executeUpdate();						
				}
			}
			for (SpaceMember member:space.getMembers()){
				membersStatement.clearParameters();
				membersStatement.setString(1, space.getId());
				membersStatement.setString(2, member.getRole().name());
				membersStatement.setString(3, member.getJID());
				membersStatement.executeUpdate();
			}
			channelsStatement.close();
			membersStatement.close();
		} catch (SQLException e){
			logger.log(Level.SEVERE, "An Exception occured while saving information in the database", e);
		}
	}

	@Override
	protected void clearDataCache(){
		try {
			Statement s = db.createStatement();
			s.executeUpdate("DELETE FROM " + SendTable.TABLE_NAME);
			s.executeUpdate("DELETE FROM " + DataTable.TABLE_NAME);
			s.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "An Exception occured while deleting information from the database", e);
		}
	}

	@Override
	protected void clearSpacesCache(){
		try{
			Statement s = db.createStatement();
			s.executeUpdate("DELETE FROM " + SpacesTable.TABLE_NAME);
			s.executeUpdate("DELETE FROM " + ChannelsTable.TABLE_NAME);
			s.executeUpdate("DELETE FROM " + MembersTable.TABLE_NAME);
			s.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "An Exception occured while deleting information from the database", e);
		}
	}

	@Override
	protected void clearSavedDataObjects(){
		try{
			Statement s = db.createStatement();
			s.executeUpdate("DELETE FROM " + DataTable.TABLE_NAME);
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "An Exception occured while deleting information from the database", e);
		}
	}
	
	@Override
	protected void updateCachedDataObjects(Set<DataModel> dataModels){
		String query = "Select * FROM " + DataTable.TABLE_NAME + ";";
		try{
			Statement s = db.createStatement();
			ResultSet rs = s.executeQuery(query);
			List<DataObject> objs = new ArrayList<DataObject>();
			while(rs.next()){
				String namespace = rs.getString(DataTable.DATA_NAMESPACE);
				String payload = rs.getString(DataTable.DATA_PAYLOAD);
				
				SAXBuilder reader = new SAXBuilder();
				StringReader in = new StringReader(payload);
				Document document = null;
				try{
					document = reader.build(in);
				} catch (JDOMException e) {
					logger.log(Level.WARNING, "An JDOMException was thrown while parsing a newly gotten item.", e);
				} catch (IOException e) {
					logger.log(Level.WARNING, "An IOException was thrown while parsing a newly gotten item.", e);
				}
				if (document == null){
					continue;
				}
				Element elem = document.getRootElement();
				DataObject obj = new DataObjectBuilder(elem, namespace).build();
				if (!dataModels.contains(obj.getDataModel())){
					objs.add(obj);
				}
			}
			if (objs.size()>0){
				StringBuilder builder = new StringBuilder("DELETE FROM " + DataTable.TABLE_NAME + " WHERE ");
				for (int i=0; i<objs.size(); i++){
					DataObject obj = objs.get(i);
					builder.append(DataTable.DATA_ID + "=" + obj.getId());
					if (i != objs.size()-1){
						builder.append(" OR ");
					}
				}
				s.executeUpdate(builder.toString());
			}
			s.close();
			rs.close();
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "An Exception occured while deleting information from the database", e);
		}
	}
	
	@Override
	protected void deleteCachedDataObjectsForSpace(String nodeId){
		try{
			Statement s = db.createStatement();
			s.executeUpdate("DELETE FROM " + DataTable.TABLE_NAME + " WHERE " + DataTable.DATA_NODE + "='" + nodeId+ "'");
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "An Exception occured while deleting information from the database", e);
		}
	}
}
