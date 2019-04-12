package de.ascendro.f4m.service.cache;

public interface CachedWithTTL extends Cached {

	Long getExpirationTime();

	Long getTimeToLive();

	void setTimeToLive(long timeToLive);

}
