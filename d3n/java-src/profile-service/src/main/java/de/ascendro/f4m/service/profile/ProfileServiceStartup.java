package de.ascendro.f4m.service.profile;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.friend.FriendManagerMessageTypes;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.profile.di.ProfileServiceModule;
import de.ascendro.f4m.service.profile.di.ProfileWebSocketModule;

/**
 * Profile Service startup class
 */
public class ProfileServiceStartup extends ServiceStartup {
	public ProfileServiceStartup(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(EventMessageTypes.SERVICE_NAME, FriendManagerMessageTypes.SERVICE_NAME,
				PaymentMessageTypes.SERVICE_NAME, AuthMessageTypes.SERVICE_NAME);
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new ProfileServiceModule(), new ProfileWebSocketModule());
	}

	public static void main(String... args) throws Exception {
		new ProfileServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	protected String getServiceName() {
		return ProfileMessageTypes.SERVICE_NAME;
	}

}
