package de.ascendro.f4m.service.game.selection;

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
import de.ascendro.f4m.service.game.engine.GameEngineMessageTypes;
import de.ascendro.f4m.service.game.selection.di.GameSelectionServiceModule;
import de.ascendro.f4m.service.game.selection.di.GameSelectionWebSocketModule;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;

/**
 * Service startup class
 * 
 */
public class GameSelectionServiceStartup extends ServiceStartup {

	public GameSelectionServiceStartup(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new GameSelectionServiceModule(), new GameSelectionWebSocketModule());
	}

	public static void main(String... args) throws Exception {
		new GameSelectionServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	protected String getServiceName() {
		return GameSelectionMessageTypes.SERVICE_NAME;
	}

	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(FriendManagerMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME,
				ProfileMessageTypes.SERVICE_NAME, EventMessageTypes.SERVICE_NAME, GameEngineMessageTypes.SERVICE_NAME,
				PaymentMessageTypes.SERVICE_NAME, AuthMessageTypes.SERVICE_NAME);
	}

}
