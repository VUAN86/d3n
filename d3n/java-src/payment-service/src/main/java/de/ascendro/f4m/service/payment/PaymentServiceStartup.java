package de.ascendro.f4m.service.payment;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.auth.AuthMessageTypes;
import de.ascendro.f4m.service.communicator.RabbitClient;
import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.handler.ClientMesageHandler;
import de.ascendro.f4m.service.handler.ServerMesageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.cache.AccountBalanceCache;
import de.ascendro.f4m.service.payment.client.AdminEmailForwarder;
import de.ascendro.f4m.service.payment.client.PaymentServiceClientMessageHandler;
import de.ascendro.f4m.service.payment.di.PaymentServiceModule;
import de.ascendro.f4m.service.payment.di.PaymentServletModule;
import de.ascendro.f4m.service.payment.di.PaymentWebSocketModule;
import de.ascendro.f4m.service.payment.manager.GameManager;
import de.ascendro.f4m.service.payment.manager.PaymentManager;
import de.ascendro.f4m.service.payment.manager.UserAccountManager;
import de.ascendro.f4m.service.payment.manager.UserPaymentManager;
import de.ascendro.f4m.service.payment.manager.impl.UserPaymentManagerImpl;
import de.ascendro.f4m.service.payment.server.PaymentServiceServerMessageHandler;
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
		return Arrays.asList(new PaymentServiceModule(), new PaymentServletModule());
	}

	public static void main(String... args) throws Exception {
		new PaymentServiceStartup(DEFAULT_STAGE).startK8S();
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
