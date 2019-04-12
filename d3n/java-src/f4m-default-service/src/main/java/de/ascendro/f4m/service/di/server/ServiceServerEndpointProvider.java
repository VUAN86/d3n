package de.ascendro.f4m.service.di.server;

import javax.inject.Provider;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

import de.ascendro.f4m.service.ServiceEndpoint;

public abstract class ServiceServerEndpointProvider<D, E, R> implements Provider<ServerEndpointConfig> {
	private final String path;
	private final Class<? extends Endpoint> serviceEndpointClass;

	public ServiceServerEndpointProvider(String path, Class<? extends Endpoint> serviceEndpointClass) {
		this.path = path;
		this.serviceEndpointClass = serviceEndpointClass;
	}

	@Override
	public ServerEndpointConfig get() {
		return ServerEndpointConfig.Builder.create(serviceEndpointClass, path)
				.configurator(new ServerEndpointConfig.Configurator() {
					@SuppressWarnings("unchecked")
					@Override
					public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
						return (T) getServiceEndpoint();
					}
				}).build();
	}

	protected abstract ServiceEndpoint<D, E, R> getServiceEndpoint();
}
