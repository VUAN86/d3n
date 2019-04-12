package de.ascendro.f4m.service.advertisement.server;


import static org.mockito.Mockito.when;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.model.AppConfigApplication;
import de.ascendro.f4m.server.profile.model.AppConfigApplicationConfiguration;
import de.ascendro.f4m.server.profile.model.AppConfigFyber;
import de.ascendro.f4m.service.advertisement.callback.FyberParameters;
import de.ascendro.f4m.service.advertisement.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class FyberEndpointTest {

    private static final String USER_ID = "userID";
    private static final String APPLICATION_ID = "applicationID";
    private static final String TENANT_ID = "tenantID";

    @Rule public ExpectedException thrown = ExpectedException.none();

    FyberEndpointCallback fyberEndpointCallback;
    FyberParameters fyberParameters;

    @Mock
    private DependencyServicesCommunicator dependencyServicesCommunicator;

    @Mock
    private AppConfig appConfig;

    @Mock
    private AppConfigApplication appConfigApplication;

    @Mock
    private AppConfigApplicationConfiguration appConfigApplicationConfiguration;

    @Mock
    private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;



    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fyberEndpointCallback =  new FyberEndpointCallback(dependencyServicesCommunicator, applicationConfigurationAerospikeDao);
    }

    @Test
    public void testBadAmount() {
        fyberParameters = new FyberParameters();

        thrown.expect(F4MFatalErrorException.class);
        thrown.expectMessage(CoreMatchers.containsString("Amount must be positive number for message"));
        fyberEndpointCallback.onAdvertisementSuccess(fyberParameters);
    }

    @Test
    public void testBadCurrency() {
        fyberParameters = new FyberParameters();
        fyberParameters.setAmount("10");

        thrown.expect(F4MFatalErrorException.class);
        thrown.expectMessage(CoreMatchers.containsString("Invalid currency for message"));
        fyberEndpointCallback.onAdvertisementSuccess(fyberParameters);
    }

    @Test
    public void testBadIdentification() {
        fyberParameters = new FyberParameters();
        fyberParameters.setAmount("10");
        fyberParameters.setCurrencyId("BONUS");

        thrown.expect(F4MFatalErrorException.class);
        thrown.expectMessage(CoreMatchers.containsString("Invalid identification information(user, application or tenant) for message"));
        fyberEndpointCallback.onAdvertisementSuccess(fyberParameters);
    }

    @Test
    public void testBadSecurity() {
        fyberParameters = new FyberParameters();
        fyberParameters.setAmount("10");
        fyberParameters.setCurrencyId("BONUS");
        fyberParameters.setUserId(USER_ID);
        fyberParameters.addParameter(FyberParameters.ExtraParameters.PUB0, APPLICATION_ID);
        fyberParameters.addParameter(FyberParameters.ExtraParameters.PUB1, TENANT_ID);

        AppConfigFyber fyberConfig = new AppConfigFyber();

        when(applicationConfigurationAerospikeDao.getAppConfiguration(TENANT_ID, APPLICATION_ID)).thenReturn(appConfig);
        when(appConfig.getApplication()).thenReturn(appConfigApplication);
        when(appConfigApplication.getConfiguration()).thenReturn(appConfigApplicationConfiguration);
        when(appConfigApplicationConfiguration.getFyber()).thenReturn(fyberConfig);

        thrown.expect(F4MFatalErrorException.class);
        thrown.expectMessage(CoreMatchers.containsString("Security check failed for message"));
        fyberEndpointCallback.onAdvertisementSuccess(fyberParameters);
    }
}
