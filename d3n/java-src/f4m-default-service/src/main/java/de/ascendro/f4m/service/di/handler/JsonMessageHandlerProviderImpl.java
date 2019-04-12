package de.ascendro.f4m.service.di.handler;

import javax.inject.Inject;

import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.session.SessionWrapperFactory;

public abstract class JsonMessageHandlerProviderImpl implements JsonMessageHandlerProvider {

	@Inject
	protected SessionWrapperFactory sessionWrapperFactory;

	@Inject
	protected JsonMessageUtil jsonMessageUtil;

	@Override
	public JsonMessageHandler get() {
		return createServiceMessageHandler();
	}

	protected abstract JsonMessageHandler createServiceMessageHandler();

}
