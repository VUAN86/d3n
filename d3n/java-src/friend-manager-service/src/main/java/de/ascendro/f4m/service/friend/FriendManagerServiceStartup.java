package de.ascendro.f4m.service.friend;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.friend.di.FriendManagerServiceModule;
import de.ascendro.f4m.service.friend.di.FriendManagerWebSocketModule;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;

public class FriendManagerServiceStartup extends ServiceStartup {

	public FriendManagerServiceStartup(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new FriendManagerServiceModule(), new FriendManagerWebSocketModule());
	}

	public static void main(String... args) throws Exception {
		new FriendManagerServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	protected String getServiceName() {
		return FriendManagerMessageTypes.SERVICE_NAME;
	}
	
	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(AuthMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME, 
				UserMessageMessageTypes.SERVICE_NAME);
	}
}
