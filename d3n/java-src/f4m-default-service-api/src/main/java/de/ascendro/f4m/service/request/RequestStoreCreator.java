package de.ascendro.f4m.service.request;

@FunctionalInterface
public interface RequestStoreCreator {
	public RequestInfo createSessionStore(long sequence);
}
