package de.ascendro.f4m.service.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceUtil {
	private static long lastGeneratedId = System.nanoTime();
	private static long idGeneratorBaseValue = 0;

	public static void setIdGeneratorBaseValue(Long propertyAsLong) {
		idGeneratorBaseValue = propertyAsLong.longValue();
	}

	public synchronized long generateId() {
		long currentId = System.nanoTime();
		if (currentId <= lastGeneratedId) {
			currentId = lastGeneratedId + 1;
		}
		lastGeneratedId = currentId;
		return currentId + idGeneratorBaseValue;
	}

	public Long getMessageTimestamp() {
		return System.currentTimeMillis();
	}

	public <T> Set<T> getIntersectionOfSets(List<Set<T>> listOfSets) {
		Set<T> intersection = new HashSet<>();
		if (!listOfSets.isEmpty()) {
			intersection = listOfSets.get(0);
			for (int i = 1; i < listOfSets.size(); i++) {
				intersection.retainAll(listOfSets.get(i));
			}
		}
		return intersection;
	}
}
