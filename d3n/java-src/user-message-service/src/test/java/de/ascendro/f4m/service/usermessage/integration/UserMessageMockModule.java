package de.ascendro.f4m.service.usermessage.integration;

import static org.mockito.Mockito.mock;

import javax.inject.Singleton;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.sns.AmazonSNS;
import com.google.inject.AbstractModule;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.server.onesignal.OneSignalWrapper;

public class UserMessageMockModule extends AbstractModule {
	@Override
	protected void configure() {
		AmazonSimpleEmailService sesClient = mock(AmazonSimpleEmailService.class);
		bind(AmazonSimpleEmailService.class).toProvider(() -> sesClient);
		AmazonSNS snsClient = mock(AmazonSNS.class);
		bind(AmazonSNS.class).toProvider(() -> snsClient);
		bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
		bind(OneSignalWrapper.class).toInstance(mock(OneSignalWrapper.class));
		bind(CommonProfileAerospikeDao.class).toInstance(mock(CommonProfileAerospikeDao.class));
	}
}
