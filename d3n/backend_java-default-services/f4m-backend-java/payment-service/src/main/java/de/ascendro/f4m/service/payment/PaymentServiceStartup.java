package de.ascendro.f4m.service.payment;

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
import de.ascendro.f4m.service.payment.payment.system.di.BalanceManagerModule;
import de.ascendro.f4m.service.payment.di.PaymentServiceModule;
import de.ascendro.f4m.service.payment.di.PaymentServletModule;
import de.ascendro.f4m.service.payment.di.PaymentWebSocketModule;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;

public class PaymentServiceStartup extends ServiceStartup {
	public PaymentServiceStartup(Stage stage) {
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
		return Arrays.asList(new PaymentServiceModule(), new PaymentWebSocketModule(), new PaymentServletModule(), new BalanceManagerModule());
	}

	public static void main(String... args) throws Exception {
		new PaymentServiceStartup(DEFAULT_STAGE).start();
	}

	@Override
	protected String getServiceName() {
		return PaymentMessageTypes.SERVICE_NAME;
	}

	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(UserMessageMessageTypes.SERVICE_NAME, AuthMessageTypes.SERVICE_NAME,
				ProfileMessageTypes.SERVICE_NAME, EventMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME);
	}
}
