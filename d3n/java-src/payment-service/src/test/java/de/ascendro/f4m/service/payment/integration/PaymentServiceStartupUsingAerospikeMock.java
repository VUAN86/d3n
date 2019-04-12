package de.ascendro.f4m.service.payment.integration;

import java.util.Arrays;

import javax.inject.Singleton;

import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.payment.PaymentServiceStartup;
import de.ascendro.f4m.service.payment.di.PaymentManagerProvider;
import de.ascendro.f4m.service.payment.manager.PaymentManager;
import de.ascendro.f4m.service.payment.rest.wrapper.AuthRestWrapper;

public class PaymentServiceStartupUsingAerospikeMock extends PaymentServiceStartup {

	public PaymentServiceStartupUsingAerospikeMock(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		final Module superModule = Modules.override(getBaseModules()).with(super.getModules());
		return Guice.createInjector(Modules.override(superModule).with(getModules()));
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(getPaymentServiceAerospikeOverrideModule());
	}

	protected AbstractModule getPaymentServiceAerospikeOverrideModule() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				final PaymentManager paymentManagerMock = Mockito.mock(PaymentManager.class);
				final CommonProfileAerospikeDao commonProfileAerospikeDaoMock = Mockito
						.mock(CommonProfileAerospikeDao.class);
				final AuthRestWrapper authRestWrapperMock = Mockito.mock(AuthRestWrapper.class);

				bind(CommonProfileAerospikeDao.class).toInstance(commonProfileAerospikeDaoMock);
				bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
				bind(PaymentManagerProvider.class).toInstance(new PaymentManagerProvider() {
					@Override
					public PaymentManager get() {
						return paymentManagerMock;
					}
				});
				bind(AuthRestWrapper.class).toInstance(authRestWrapperMock);
			}
		};
	}

}
