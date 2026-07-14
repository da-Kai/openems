package io.openems.backend.uiwebsocket.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WsSessionRegistry {

	private final ConcurrentHashMap<UUID, WsData> wsDataById = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Set<WsData>> wsDataByEdgeId = new ConcurrentHashMap<>();

	WsSessionRegistry() {
	}

	public void clear() {
		this.wsDataById.clear();
		this.wsDataByEdgeId.clear();
	}

	/**
	 * Get WsData by id.
	 *
	 * @param id to get
	 * @return WsData
	 */
	public WsData getWsData(UUID id) {
		return this.wsDataById.get(id);
	}

	/**
	 * Get all WsData subscribed to edgeId.
	 *
	 * @param edgeId to get
	 * @return WsData
	 */
	public Set<WsData> getWsDataByEdgeId(String edgeId) {
		return this.wsDataByEdgeId.get(edgeId);
	}

	public void registerWsData(WsData wsData) {
		this.wsDataById.put(wsData.getId(), wsData);
	}

	public void unregisterWsData(WsData wsData) {
		this.wsDataById.remove(wsData.getId());
		for (var edgeId : new HashSet<>(wsData.getSubscribedEdges())) {
			this.wsDataByEdgeId.compute(edgeId, (id, wsDataSet) -> {
				if (wsDataSet == null) {
					return null;
				}
				wsDataSet.remove(wsData);
				return wsDataSet.isEmpty() ? null : wsDataSet;
			});
		}
	}

	public void registerWsDataForEdgeId(String edgeId, WsData wsData) {
		this.wsDataByEdgeId.compute(edgeId, (id, wsDataSet) -> {
			if (wsDataSet == null) {
				wsDataSet = ConcurrentHashMap.newKeySet();
			}
			wsDataSet.add(wsData);
			return wsDataSet;
		});
	}

	public void unregisterWsDataForEdgeId(String edgeId, WsData wsData) {
		this.wsDataByEdgeId.compute(edgeId, (id, wsDataSet) -> {
			if (wsDataSet == null) {
				return null;
			}
			wsDataSet.remove(wsData);
			return wsDataSet.isEmpty() ? null : wsDataSet;
		});
	}


}
