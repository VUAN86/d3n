package de.ascendro.f4m.service.usermessage.di;

import javax.inject.Inject;
import javax.inject.Provider;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;

import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;

/**
 * Provider for creating Amazon service instances.
 */
public class AmazonSimpleNotificationServiceProvider implements Provider<AmazonSNS> {

	private UserMessageConfig config;

	@Inject
	public AmazonSimpleNotificationServiceProvider(UserMessageConfig config) {
		this.config = config;
	}

	@Override
	public AmazonSNS get() {
		AWSCredentials credentials = config.getCredentials();
		AmazonSNS sesClient = new AmazonSNSClient(credentials);
		sesClient.setRegion(config.getRegion());
		return sesClient;
	}
}
