package de.ascendro.f4m.service.usermessage;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.usermessage.di.UserMessageServiceModule;
import de.ascendro.f4m.service.usermessage.di.UserMessageWebSocketModule;

public class UserMessageServiceStartup extends ServiceStartup {

	public UserMessageServiceStartup(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(getModule());
	}

	protected Module getModule() {
		return Modules.override(super.getModules()).with(getModules());
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new UserMessageServiceModule(), new UserMessageWebSocketModule());
	}

	public static void main(String... args) throws Exception {
		new UserMessageServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	protected String getServiceName() {
		return UserMessageMessageTypes.SERVICE_NAME;
	}

	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(ProfileMessageTypes.SERVICE_NAME);
	}
}
