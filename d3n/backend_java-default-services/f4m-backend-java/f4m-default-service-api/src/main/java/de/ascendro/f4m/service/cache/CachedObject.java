package de.ascendro.f4m.service.cache;

public class CachedObject implements CachedWithTTL {
	protected Long timeToLive;
	protected long lastAccessTime;

	public CachedObject(long timeToLive) {
		this.timeToLive = timeToLive;
		refreshLastAccess();
	}

	public CachedObject() {
		refreshLastAccess();
	}

	@Override
	public void refreshLastAccess() {
		this.lastAccessTime = System.currentTimeMillis();
	}

	@Override
	public boolean isExpired() {
		return getExpirationTime() < System.currentTimeMillis();
	}

	@Override
	public Long getExpirationTime() {
		final long expirationTime;
		if (timeToLive != null) {
			expirationTime = lastAccessTime + timeToLive;
		} else {
			expirationTime = 0;
		}
		return expirationTime;
	}

	@Override
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	@Override
	public Long getTimeToLive() {
		return timeToLive;
	}
}
