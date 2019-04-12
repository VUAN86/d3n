package de.ascendro.f4m.service.promocode;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.payment.PaymentMessageTypes;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.promocode.di.PromocodeServiceModule;
import de.ascendro.f4m.service.promocode.di.PromocodeWebSocketModule;


/**
 * Promocode Service startup class
 */
public class PromocodeServiceStartup extends ServiceStartup {
	public PromocodeServiceStartup(Stage stage) {
		super(stage);
	}
	
	public static void main(String... args) throws Exception {
		new PromocodeServiceStartup(DEFAULT_STAGE).start();
	}
	
	@Override
	public Injector createInjector(Stage stage) {
		return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
	}

	@Override
	public void start() throws Exception {
		super.start();
		discoverProfileService();
		discoverPaymentService();
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
		return Arrays.asList(new PromocodeServiceModule(), new PromocodeWebSocketModule());
	}

	@Override
	protected String getServiceName() {
		return PromocodeMessageTypes.SERVICE_NAME;
	}

}
