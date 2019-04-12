package de.ascendro.f4m.service.cache;

public interface Cached {
	boolean isExpired();

	void refreshLastAccess();
}
