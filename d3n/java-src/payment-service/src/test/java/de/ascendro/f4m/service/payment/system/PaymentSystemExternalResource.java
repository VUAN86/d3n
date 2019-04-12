package de.ascendro.f4m.service.payment.system;

import static org.mockito.Mockito.spy;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.rules.ExternalResource;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class PaymentSystemExternalResource extends ExternalResource {
	private JerseyTest jerseyTestServer;
	private AccountsResource accountsResource;
	private AccountTransactionsResource accountTransactionsResource;
	private UsersResource usersResource;
	private PaymentTransactionsResource paymentTransactionsResource;

	@Override
	protected void before() throws Throwable {
		jerseyTestServer = new JerseyTest() {
			@Override
			protected Application configure() {
				//here is how to override port:
				forceSet(TestProperties.CONTAINER_PORT, String.valueOf(TestProperties.DEFAULT_CONTAINER_PORT));
				ResourceConfig resourceConfig = new ResourceConfig();
				//spy is used to have default behaviour with possibility to override it
				accountsResource = spy(new AccountsResource());
				accountTransactionsResource = spy(new AccountTransactionsResource());
				usersResource = spy(new UsersResource());
				paymentTransactionsResource = spy(new PaymentTransactionsResource());
				resourceConfig.registerInstances(new CurrenciesResource(), new ExchangeRatesResource(),
						accountsResource, accountTransactionsResource, usersResource, paymentTransactionsResource);
				return resourceConfig;
			}
		};
		jerseyTestServer.setUp();
	}

	@Override
	protected void after() {
		try {
			jerseyTestServer.tearDown();
		} catch (Exception e) {
			throw new F4MFatalErrorException("Could not shut down jersey test server", e);
		}
	}

	public JerseyTest getJerseyTestServer() {
		return jerseyTestServer;
	}

	public AccountsResource getAccountsResource() {
		return accountsResource;
	}
	
	public AccountTransactionsResource getAccountTransactionsResource() {
		return accountTransactionsResource;
	}
	
	public UsersResource getUsersResource() {
		return usersResource;
	}
	
	public PaymentTransactionsResource getPaymentTransactionsResource() {
		return paymentTransactionsResource;
	}
}
