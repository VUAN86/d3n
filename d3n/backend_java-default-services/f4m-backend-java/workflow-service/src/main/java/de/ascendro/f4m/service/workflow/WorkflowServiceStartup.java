package de.ascendro.f4m.service.workflow;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.workflow.di.WorkflowServiceModule;
import de.ascendro.f4m.service.workflow.di.WorkflowWebSocketModule;

public class WorkflowServiceStartup extends ServiceStartup {

	public WorkflowServiceStartup(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new WorkflowServiceModule(), new WorkflowWebSocketModule());
	}

	public static void main(String... args) throws Exception {
		new WorkflowServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	protected String getServiceName() {
		return WorkflowMessageTypes.SERVICE_NAME;
	}
	
	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(EventMessageTypes.SERVICE_NAME);
	}

}
