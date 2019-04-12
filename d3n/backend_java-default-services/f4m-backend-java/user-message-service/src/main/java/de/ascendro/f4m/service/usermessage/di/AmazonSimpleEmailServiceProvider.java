package de.ascendro.f4m.service.usermessage.di;

import javax.inject.Inject;
import javax.inject.Provider;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;

import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;

/**
 * Provider for creating Amazon service instances.
 */
public class AmazonSimpleEmailServiceProvider implements Provider<AmazonSimpleEmailService> {

	private UserMessageConfig config;

	@Inject
	public AmazonSimpleEmailServiceProvider(UserMessageConfig config) {
		this.config = config;
	}

	@Override
	public AmazonSimpleEmailService get() {
		AWSCredentials credentials = config.getCredentials();
		AmazonSimpleEmailServiceClient sesClient = new AmazonSimpleEmailServiceClient(credentials);
		sesClient.setRegion(config.getRegion());
		return sesClient;
	}
}
