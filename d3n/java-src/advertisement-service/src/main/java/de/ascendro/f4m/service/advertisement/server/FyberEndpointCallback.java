package de.ascendro.f4m.service.advertisement.server;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.model.AppConfigFyber;
import de.ascendro.f4m.service.advertisement.callback.FyberParameters;
import de.ascendro.f4m.service.advertisement.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.advertisement.client.RewardPayoutRequestInfo;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class FyberEndpointCallback extends HttpServlet {
    private static final long serialVersionUID = -8949698598310135033L;
    private static final Logger LOGGER = LoggerFactory.getLogger(FyberEndpointCallback.class);

    private final DependencyServicesCommunicator dependencyServicesCommunicator;
    private final ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;

    @Inject
    public FyberEndpointCallback(DependencyServicesCommunicator dependencyServicesCommunicator,
                                 ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao) {
        this.dependencyServicesCommunicator = dependencyServicesCommunicator;
        this.applicationConfigurationAerospikeDao = applicationConfigurationAerospikeDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        FyberParameters parameters = readParameters(req);
        LOGGER.info("Success callback received from Fyber for transaction {}", parameters.getTransactionId());
        try {
            onAdvertisementSuccess(parameters);
        } catch (Exception e) {
            LOGGER.error("Error processing Fyber callback", e);
        }
    }

    protected void onAdvertisementSuccess(FyberParameters parameters) {
        RewardPayoutRequestInfo rewardPayoutRequestInfo = new RewardPayoutRequestInfo();
        rewardPayoutRequestInfo.setFyberTransactionId(parameters.getTransactionId());

        if (parameters.getAmount().signum() > 0) {
            rewardPayoutRequestInfo.setAmount(parameters.getAmount());
        } else {
            throw new F4MFatalErrorException("Amount must be positive number for message " + parameters.toString());
        }
        if (parameters.getCurrency() != null) {
            rewardPayoutRequestInfo.setCurrency(parameters.getCurrency());
        } else {
            throw new F4MFatalErrorException("Invalid currency for message " + parameters.toString());
        }

        if (securityCheck(parameters)) {
            dependencyServicesCommunicator.initiateRewardPayment(rewardPayoutRequestInfo);
        } else {
            throw new F4MFatalErrorException("Security check failed for message " + parameters.toString());
        }
    }

    private boolean securityCheck(FyberParameters parameters) {
        if (parameters.hasIdentificationParamas()) {
            AppConfig appConfig = applicationConfigurationAerospikeDao.getAppConfiguration(parameters.getTenantId(),
                    parameters.getApplicationId());

            AppConfigFyber fyberConfig = appConfig.getApplication()
                    .getConfiguration().getFyber();

            String sha1String = fyberConfig.getRewardHandlingSecurityToken() + parameters.getUserId()
                    + parameters.getAmount() + parameters.getTransactionId() + parameters.getApplicationId();

            String sah1Signature = DigestUtils.sha1Hex(sha1String);

            boolean securityCheck = sah1Signature.equals(parameters.getSignature());

            if (!securityCheck) {
                LOGGER.debug("Fyber RewardHandlingSecurityToken = {}", fyberConfig.getRewardHandlingSecurityToken());
                LOGGER.debug("Fyber SHA1 String {}", sha1String);
                LOGGER.debug("Fyber SHA1 Signature {}", sah1Signature);
                LOGGER.debug("Fyber Message Signature {}", parameters.getSignature());
            }

            return securityCheck;

        } else {
            throw new F4MFatalErrorException("Invalid identification information(user, application or tenant) for message " + parameters.toString());
        }
    }

    protected FyberParameters readParameters(HttpServletRequest req) {
        FyberParameters parameters = new FyberParameters();

        parameters.setTransactionId(req.getParameter(FyberParameters.PARAM_TRANSACTION_ID));
        parameters.setUserId(req.getParameter(FyberParameters.PARAM_USER_ID));
        parameters.setSignature(req.getParameter(FyberParameters.PARAM_SIGNATURE_ID));
        parameters.setAmount(req.getParameter(FyberParameters.PARAM_AMOUNT));
        parameters.setCurrencyName(req.getParameter(FyberParameters.PARAM_CURRENCY_NAME));
        parameters.setCurrencyId(req.getParameter(FyberParameters.PARAM_CURRENCY_ID));

        EnumSet.allOf(FyberParameters.ExtraParameters.class).forEach(p -> parameters.addParameter(p, req.getParameter(p.getParamName())));

        return parameters;
    }
}

