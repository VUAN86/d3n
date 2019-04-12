package de.ascendro.f4m.service.workflow.di;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.annotation.ServerMessageHandler;
import de.ascendro.f4m.service.di.WebSocketModule;
import de.ascendro.f4m.service.di.handler.DefaultJsonMessageHandlerProviderImpl;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.json.handler.JsonMessageHandler;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.workflow.WorkflowMessageTypeMapper;
import de.ascendro.f4m.service.workflow.client.WorkflowServiceClientMessageHandler;
import de.ascendro.f4m.service.workflow.model.schema.WorkflowMessageSchemaMapper;
import de.ascendro.f4m.service.workflow.server.WorkflowServiceServerMessageHandler;
import de.ascendro.f4m.service.workflow.utils.WorkflowWrapper;

public class WorkflowWebSocketModule extends WebSocketModule {
	@Override
	protected void configure() {
		bind(WorkflowMessageTypeMapper.class).in(Singleton.class);
		bind(JsonMessageTypeMap.class).to(WorkflowDefaultMessageTypeMapper.class).in(Singleton.class);
        bind(JsonMessageSchemaMap.class).to(WorkflowMessageSchemaMapper.class).in(Singleton.class);

		//Client
		bind(JsonMessageHandlerProvider.class).annotatedWith(ClientMessageHandler.class).to(
				WorkflowServiceClientMessageHandlerProvider.class);

		//Server		
		bind(JsonMessageHandlerProvider.class).annotatedWith(ServerMessageHandler.class).to(
				WorkflowServiceServerMessageHandlerProvider.class);
	}

	static class WorkflowServiceClientMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {
		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new WorkflowServiceClientMessageHandler();
		}
	}
	
	static class WorkflowServiceServerMessageHandlerProvider extends DefaultJsonMessageHandlerProviderImpl {

		@Inject
		private WorkflowWrapper workflowWrapper;
		
		@Override
		protected JsonMessageHandler createServiceMessageHandler() {
			return new WorkflowServiceServerMessageHandler(workflowWrapper);
		}
	}
}
