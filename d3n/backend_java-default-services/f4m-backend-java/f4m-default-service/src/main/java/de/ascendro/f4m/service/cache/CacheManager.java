package de.ascendro.f4m.service.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import de.ascendro.f4m.service.logging.LoggingUtil;

public final class CacheManager<K, V extends Cached> {

	private final Map<K, V> cacheHashMap = new ConcurrentHashMap<>();

	private final long cleanUpDelay;

	private final Timer scheduler = new Timer("CacheManager-scheduler");
	private final CacheCleanUpTask cacheCleanUpTask;

	public CacheManager(long cleanUpDealy, LoggingUtil loggingUtil) {
		this.cleanUpDelay = cleanUpDealy;
		this.cacheCleanUpTask = new CacheCleanUpTask(cacheHashMap, loggingUtil);
	}

	public void schedule() {
		scheduler.schedule(cacheCleanUpTask, cleanUpDelay, cleanUpDelay);
	}

	public void destroy() {
		clear();
		scheduler.cancel();
	}

	public void put(K key, V value) {
		cacheHashMap.put(key, value);
	}

	public void clear() {
		cacheHashMap.clear();
	}

	public V getAndRefresh(K key) {
		V cachedValue = cacheHashMap.get(key);
		if (cachedValue != null) {
			cachedValue.refreshLastAccess();
		}
		return cachedValue;
	}

	public boolean contains(K key) {
		return cacheHashMap.containsKey(key);
	}

	public V remove(K key) {
		return cacheHashMap.remove(key);
	}

	public Collection<V> getValues() {
		return cacheHashMap.values();
	}

	public int size() {
		return cacheHashMap.size();
	}

	@Override
	public String toString() {
		return "CacheManager{" +
				"cacheHashMap=" + cacheHashMap +
				", cleanUpDelay=" + cleanUpDelay +
				", scheduler=" + scheduler +
				", cacheCleanUpTask=" + cacheCleanUpTask +
				'}';
	}
}
