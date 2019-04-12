package de.ascendro.f4m.service.tombola;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.communicator.RabbitClient;
import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.handler.ClientMesageHandler;
import de.ascendro.f4m.service.handler.ServerMesageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.tombola.di.TombolaServiceModule;
import de.ascendro.f4m.service.tombola.di.TombolaWebSocketModule;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;
import de.ascendro.f4m.service.voucher.VoucherMessageTypes;

/**
 * Tombola Service startup class
 */
public class TombolaServiceStartup extends ServiceStartup {
	public TombolaServiceStartup(Stage stage) {
		super(stage);
	}

	public static void main(String... args) throws Exception {
		new TombolaServiceStartup(DEFAULT_STAGE).startK8S();
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new TombolaServiceModule());
	}

	@Override
	protected String getServiceName() {
		return TombolaMessageTypes.SERVICE_NAME;
	}

	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(PaymentMessageTypes.SERVICE_NAME, VoucherMessageTypes.SERVICE_NAME,
				ProfileMessageTypes.SERVICE_NAME, EventMessageTypes.SERVICE_NAME, UserMessageMessageTypes.SERVICE_NAME);
	}

	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}
}
