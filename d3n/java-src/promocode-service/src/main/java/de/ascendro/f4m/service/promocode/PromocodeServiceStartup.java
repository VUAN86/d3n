package de.ascendro.f4m.service.promocode;

import java.util.Arrays;
import java.util.List;

import com.google.inject.*;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.communicator.RabbitClient;
import de.ascendro.f4m.service.communicator.RabbitClientConfig;
import de.ascendro.f4m.service.communicator.RabbitClientSender;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.handler.ClientMesageHandler;
import de.ascendro.f4m.service.handler.MessageHandler;
import de.ascendro.f4m.service.handler.ServerMesageHandler;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.promocode.client.PromocodeServiceClientMessageHandler;
import de.ascendro.f4m.service.promocode.server.PromocodeServiceServerMessageHandler;
import de.ascendro.f4m.service.promocode.util.PromocodeManager;
import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.promocode.di.PromocodeServiceModule;
import de.ascendro.f4m.service.promocode.di.PromocodeWebSocketModule;

import javax.inject.Inject;


/**
 * Promocode Service startup class
 */
public class PromocodeServiceStartup extends ServiceStartup {

//	private PromocodeManager promocodeManager;
//	private TransactionLogAerospikeDao transactionLogAerospikeDao;

	public PromocodeServiceStartup(Stage stage) {
		super(stage);
	}
	
	public static void main(String... args) throws Exception {
		new PromocodeServiceStartup(DEFAULT_STAGE).startK8S();
	}
	
	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	/**
	 * Requests Profile Service info from ServiceRegistry
	 * 
	 * @throws F4MNoServiceRegistrySpecifiedException
	 *             - no Service Registry to contact
	 */
	public void discoverProfileService() throws F4MNoServiceRegistrySpecifiedException {
		final ServiceRegistryClient serviceRegsitry = getInjector().getInstance(ServiceRegistryClient.class);
		serviceRegsitry.requestServiceConnectionInformation(ProfileMessageTypes.SERVICE_NAME);
	}

	/**
	 * Requests Payment Service info from ServiceRegistry
	 *
	 * @throws F4MNoServiceRegistrySpecifiedException
	 *             - no Service Registry to contact
	 */
	public void discoverPaymentService() throws F4MNoServiceRegistrySpecifiedException {
		final ServiceRegistryClient serviceRegsitry = getInjector().getInstance(ServiceRegistryClient.class);
		serviceRegsitry.requestServiceConnectionInformation(PaymentMessageTypes.SERVICE_NAME);
	}
	
	@Override
	protected List<String> getDependentServiceNames() {
		return Arrays.asList(ProfileMessageTypes.SERVICE_NAME, PaymentMessageTypes.SERVICE_NAME);
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(new PromocodeServiceModule());
	}

	@Override
	protected String getServiceName() {
		return PromocodeMessageTypes.SERVICE_NAME;
	}

}
