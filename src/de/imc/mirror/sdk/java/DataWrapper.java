package de.imc.mirror.sdk.java;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smackx.pubsub.SimplePayload;

import de.imc.mirror.sdk.DataModel;
import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.Space;
import de.imc.mirror.sdk.java.data.DatabaseConfig;

/**
 * Abstract wrapper for database access.
 * It is used internally by space and data handler.
 * {@link DataHandler}
 * {@link SpaceHandler}
 * @author Mach
 *
 */
public abstract class DataWrapper {
	
	private static DataWrapper instance;	

	/**
	 * Returns a previously created datawrapper instance.
	 * @return A datawrapper instance or null if no datawrapper was created.
	 */
	protected static DataWrapper getInstance(){
		return instance;
	}
	
	/**
	 * Creates a new datawrapper instance.
	 * @param dbConfig The config which determines what Databasetype is used.
	 * @return A datawrapper instance.
	 */
	protected static DataWrapper getInstance(DatabaseConfig dbConfig){
		if (DatabaseConfig.Type.NONE == dbConfig.getType()){
			instance = new DataWrapperIntern();
		}
		else instance = new DataWrapperExtern(dbConfig);
		return instance;
	}

	/**
	 * Deletes all cached spaces for the user.
	 * @param user The user to delete the spaces for.
	 */
	protected abstract void deleteCachedSpacesForUser(String user);

	/**
	 * Deletes all cached Information of a space.
	 * @param spaceId The id of the space.
	 */
	protected abstract void deleteCachedSpace(String spaceId);

	/**
	 * Gets all ids of the spaces which are cached for the given user.
	 * @param user The user to get the spaceIds for.
	 * @return A list of spaceIds.
	 */
	protected abstract List<String> getSpaceIdsForUser(String user);

	/**
	 * Saves a space and all infos of it. The id, name, pubsubnode, jid 
	 * and pubsub service for the node mustn't be null.
	 * @param space The space to cache.
	 * @param user The user to cache the space for.
	 */
	protected abstract void saveSpace(Space space, String user);

	/**
	 * Gets all spaces that are cached for the user.
	 * @param user The user to get the spaces for.
	 * @return A list of spaces.
	 */
	protected abstract List<Space> getCachedSpacesForUser(String user);

	/**
	 * Saves a payload which can't be send.
	 * @param user The user to send the payload.
	 * @param payloadId The id of the payload which should be send.
	 * @param spaceId The id of the node to send the payload to.
	 * @param payload The payload to send.
	 */
	protected abstract void savePayloadToSend(String user, String payloadId,
			String spaceId, SimplePayload payload);

	/**
	 * Gets all payloads to send for the given user.
	 * @param user The user to get the payloads for.
	 * @return A map consisting of the payloadIds and the corresponding simplepayloads.
	 */
	protected abstract Map<String, SimplePayload> getPayloadsToSend(String user);

	/**
	 * Gets the space to send a payload to.
	 * @param id The id of the payload to get the node for.
	 * @return The id of the space.
	 */
	protected abstract String getSpaceForPayload(String id);

	/**
	 * Deletes all entries of the sendcache for an user.
	 * @param user The user to delete entries for.
	 */
	protected abstract void clearSendCache(String user);

	/**
	 * Checks if an item was already cached.
	 * @param id The id of the item to check.
	 * @return If the item was already cached.
	 */
	protected abstract boolean isDataObjectAlreadyCached(String id);

	/**
	 * Saves a item.
	 * @param nodeId The id of the node the item is from.
	 * @param obj The data object to save.
	 */
	protected abstract void saveDataObject(String nodeId, DataObject obj, String id);

	/**
	 * Gets all cached items.
	 * @param nodeId The id of the node to get the items for.
	 * @return A list of all cached items.
	 */
	protected abstract List<DataObject> getCachedDataObjects(
			String nodeId);

	/**
	 * Saves all given spaces in the local cache for the given user.
	 * @param spaces The spaces to save.
	 * @param user The user to save the spaces for.
	 */
	protected abstract void saveSpaces(List<Space> spaces, String user);

	/**
	 * Updates the cached information for a space.
	 * @param space The space to update the information for.
	 */
	protected abstract void updateCachedSpaceInformation(Space space);

	/**
	 * Checks if a space with the given id is already cached.
	 * @param spaceId The spaceid to look for.
	 * @return If a entry was found or not.
	 */
	protected abstract boolean isSpaceAlreadyCached(String spaceId);

	/**
	 * Deletes all sent, received and to-be-send data currently saved.
	 */
	protected abstract void clearDataCache();

	/**
	 * Deletes all saved spaces-information.
	 */
	protected abstract void clearSpacesCache();
	
	/**
	 * Deletes all saved DataObjects.
	 */
	protected abstract void clearSavedDataObjects();
	
	/**
	 * Deletes all DataObjects for a specific Space.
	 * @param nodeId The id of the pubsubnode of the Space.
	 */
	protected abstract void deleteCachedDataObjectsForSpace(String nodeId);
	
	/**
	 * Deletes all DataObjects which doesn't implement the given datamodels.
	 * @param dataModels The datamodels to check against.
	 */
	protected abstract void updateCachedDataObjects(Set<DataModel> dataModels);

}