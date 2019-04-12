package de.ascendro.f4m.service.game.engine;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.game.engine.di.GameEngineServiceModule;
import de.ascendro.f4m.service.game.engine.di.GameEngineWebSocketModule;
import de.ascendro.f4m.service.game.engine.history.ActiveGameTimer;
import de.ascendro.f4m.service.game.selection.GameSelectionMessageTypes;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.winning.WinningMessageTypes;

public class GameEngineServiceStartup extends ServiceStartup {

	public GameEngineServiceStartup(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new GameEngineServiceModule(), new GameEngineWebSocketModule());
	}

	public static void main(String... args) throws Exception {
		new GameEngineServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(EventMessageTypes.SERVICE_NAME, ResultEngineMessageTypes.SERVICE_NAME,
				ProfileMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME, WinningMessageTypes.SERVICE_NAME,
				GameSelectionMessageTypes.SERVICE_NAME, VoucherMessageTypes.SERVICE_NAME);
	}
	
	@Override
	public void start() throws Exception {
		super.start();
		scheduleActiveGameInstanceTimer();
	}

	private void scheduleActiveGameInstanceTimer() {
		getInjector().getInstance(ActiveGameTimer.class).scheduleActiveGameCleanUpFrequently();
	}

	@Override
	protected String getServiceName() {
		return GameEngineMessageTypes.SERVICE_NAME;
	}

}
