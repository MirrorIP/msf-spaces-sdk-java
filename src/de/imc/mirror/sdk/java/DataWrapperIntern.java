package de.imc.mirror.sdk.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smackx.pubsub.SimplePayload;

import de.imc.mirror.sdk.DataModel;
import de.imc.mirror.sdk.DataObject;
import de.imc.mirror.sdk.Space;

/**
 * In-memory implementation to cache offline data.
 * Used internally by both space and data handler.
 * 
 * {@link DataHandler}
 * {@link SpaceHandler}
 * @author mach
 *
 */
public class DataWrapperIntern extends DataWrapper {
	
	private Map<String, Space> spacesCache;
	private Map<String, List<String>> userToSpaceIds;
	private Map<String, SimplePayload> sendCache;
	private Map<String, List<String>> userToPayloadId;
	private Map<String, String> payloadIdToSpaceId;
	private Map<String, List<DataObject>> dataCache;
	
	protected DataWrapperIntern(){
		spacesCache = new HashMap<String, Space>();
		userToSpaceIds = new HashMap<String, List<String>>();
		sendCache = new HashMap<String, SimplePayload>();
		userToPayloadId = new HashMap<String, List<String>>();
		payloadIdToSpaceId = new HashMap<String, String>();
		dataCache = new HashMap<String, List<DataObject>>();
	}

	@Override
	protected void deleteCachedSpacesForUser(String user) {
		spacesCache.remove(user);
	}

	@Override
	protected void deleteCachedSpace(String spaceId) {
		Set<String> keys = userToSpaceIds.keySet();
		if (keys == null) return;
		for (String key:keys){
			List<String> spaceIds = userToSpaceIds.get(key);
			spaceIds.remove(spaceId);
			userToSpaceIds.put(key, spaceIds);
		}
		spacesCache.remove(spaceId);
	}

	@Override
	protected List<String> getSpaceIdsForUser(String user) {
		List<String> spaceIds = userToSpaceIds.get(user);
		return Collections.unmodifiableList(spaceIds);
	}

	@Override
	protected void saveSpace(Space space, String user) {
		List<String> spaceIds = userToSpaceIds.get(user);
		if (spaceIds == null){
			spaceIds = new ArrayList<String>();
		}
		spaceIds.add(space.getId());
		userToSpaceIds.put(user, spaceIds);
		spacesCache.put(space.getId(), space);
	}

	@Override
	protected List<Space> getCachedSpacesForUser(String user) {
		List<String> spaceIds = userToSpaceIds.get(user);
		List<Space> spaces = new ArrayList<Space>();
		if (spaceIds == null) return spaces;
		for (String spaceId:spaceIds){
			spaces.add(spacesCache.get(spaceId));
		}
		return Collections.unmodifiableList(spaces);
	}

	@Override
	protected void savePayloadToSend(String user, String payloadId,
			String spaceId, SimplePayload payload) {
		List<String> ids = userToPayloadId.get(user);
		if (ids == null){
			ids = new ArrayList<String>();
		}
		ids.add(payloadId);
		userToPayloadId.put(user, ids);
		sendCache.put(payloadId, payload);
		payloadIdToSpaceId.put(payloadId, spaceId);
	}

	@Override
	protected Map<String, SimplePayload> getPayloadsToSend(String user) {
		List<String> payloadIds = userToPayloadId.get(user);
		Map<String, SimplePayload> payloads = new HashMap<String, SimplePayload>();
		if (payloadIds == null){
			return payloads;
		}
		for (String id:payloadIds){
			payloads.put(id, sendCache.get(id));
		}
		return Collections.unmodifiableMap(payloads);
	}

	@Override
	protected String getSpaceForPayload(String id) {
		return payloadIdToSpaceId.get(id);
	}

	@Override
	protected void clearSendCache(String user) {
		List<String> ids = userToPayloadId.get(user);
		if (ids == null) return;
		for (String id:ids){
			payloadIdToSpaceId.remove(id);
			sendCache.remove(id);
		}
		userToPayloadId.remove(user);
	}

	@Override
	protected boolean isDataObjectAlreadyCached(String id) {
		Collection<List<DataObject>> objLists = dataCache.values();
		if (objLists == null) return false;
		for (List<DataObject> objs:objLists){
			for (DataObject obj:objs){
				if (obj.getId().equals(id)){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void saveDataObject(String nodeId, DataObject obj, String id) {
		List<DataObject> items = dataCache.get(nodeId);
		if (items == null){
			items = new ArrayList<DataObject>();
		}
		items.add(obj);
		dataCache.put(nodeId, items);
	}

	@Override
	protected List<DataObject> getCachedDataObjects(String nodeId) {
		List<DataObject> items = dataCache.get(nodeId);
		if (items == null) return new ArrayList<DataObject>();
		return Collections.unmodifiableList(items);
	}

	@Override
	protected void saveSpaces(List<Space> spaces, String user) {
		List<String> spaceIds = new ArrayList<String>();
		for (Space space:spaces){
			spaceIds.add(space.getId());
			spacesCache.put(space.getId(), space);
		}
		userToSpaceIds.put(user, spaceIds);
	}

	@Override
	protected void updateCachedSpaceInformation(Space space) {
		spacesCache.put(space.getId(), space);
	}

	@Override
	protected boolean isSpaceAlreadyCached(String spaceId) {
		return spacesCache.containsKey(spaceId);
	}

	@Override
	protected void clearDataCache() {
		sendCache = new HashMap<String, SimplePayload>();
		userToPayloadId = new HashMap<String, List<String>>();
		payloadIdToSpaceId = new HashMap<String, String>();
		dataCache = new HashMap<String, List<DataObject>>();
	}

	@Override
	protected void clearSpacesCache() {
		spacesCache = new HashMap<String, Space>();
		userToSpaceIds = new HashMap<String, List<String>>();
	}

	@Override
	protected void clearSavedDataObjects(){
		dataCache = new HashMap<String, List<DataObject>>();
	}
	
	@Override
	protected void updateCachedDataObjects(Set<DataModel> dataModels){
		Set<String> keys = dataCache.keySet();
		for (String key:keys){
			List<DataObject> objs = dataCache.get(key);
			List<DataObject> newObjs = new ArrayList<DataObject>();
			for (DataObject obj:objs){
				if (dataModels.contains(obj.getDataModel())){
					newObjs.add(obj);
				}
			}
			dataCache.put(key, newObjs);
		}
	}
	
	@Override
	protected void deleteCachedDataObjectsForSpace(String nodeId){
		dataCache.remove(nodeId);
	}

}
