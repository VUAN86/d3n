package de.ascendro.f4m.service.winning;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.result.engine.ResultEngineMessageTypes;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;
import de.ascendro.f4m.service.winning.di.WinningServiceModule;
import de.ascendro.f4m.service.winning.di.WinningWebSocketModule;

/**
 * Winning Service startup class
 */
public class WinningServiceStartup extends ServiceStartup {
	public WinningServiceStartup(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new WinningServiceModule(), new WinningWebSocketModule());
	}

	public static void main(String... args) throws Exception {
		new WinningServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	protected String getServiceName() {
		return WinningMessageTypes.SERVICE_NAME;
	}
	
	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(ResultEngineMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME,
				VoucherMessageTypes.SERVICE_NAME, EventMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME);
	}

}
