package de.ascendro.f4m.service.payment.manager;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import de.ascendro.f4m.server.analytics.model.PaymentEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.F4MPaymentException;
import de.ascendro.f4m.service.payment.model.TransferFundsRequest;

public class AnalyticsEventManager {
    private final Tracker tracker;
    private Gson gson;

    @Inject
    public AnalyticsEventManager(GsonProvider gsonProvider, Tracker tracker) {
        this.tracker = tracker;
        gson = gsonProvider.get();
    }

	public void savePaymentEvent(TransferFundsRequest request, ClientInfo clientInfo) {

		PaymentEvent paymentEvent = new PaymentEvent();
		paymentEvent.setPaymentDetailsJSON(gson.toJson(request.getPaymentDetails()));
		if (request.getPaymentDetails() != null && request.getPaymentDetails().getGameId() != null) {
			paymentEvent.setGameId(request.getPaymentDetails().getGameId());
		}
		paymentEvent.setPaymentAmount(request.getAmount());
		if (clientInfo == null) {
			clientInfo = new ClientInfo();
		}
		if(request.getPaymentDetails()!=null) {
			clientInfo.setAppId(request.getPaymentDetails().getAppId());
		}

		clientInfo.setTenantId(request.getTenantId());

		if (StringUtils.isNotBlank(request.getFromProfileId())) {
			clientInfo.setUserId(request.getFromProfileId());
		} else if (StringUtils.isNotBlank(request.getToProfileId())) {
			clientInfo.setUserId(request.getToProfileId());
			paymentEvent.setPaymentReceived(true);
		} else {
			throw new F4MPaymentException("No user found in payment loadOrWithdrawWithoutCoverage");
		}

		tracker.addEvent(clientInfo, paymentEvent);
	}
}
