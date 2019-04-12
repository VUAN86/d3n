package de.ascendro.f4m.service.promocode.util;

import java.math.BigDecimal;
import java.util.Map;
import java.util.*;

import javax.inject.Inject;

import de.ascendro.f4m.server.analytics.model.PaymentEvent;
import de.ascendro.f4m.server.analytics.model.PromoCodeEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.promocode.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.promocode.dao.PromocodeAerospikeDao;
import de.ascendro.f4m.service.promocode.model.UserPromocode;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileIdentifierType;

/**
 * Utilities to process Promocode data.
 */
public class PromocodeManager {

	private final CommonProfileAerospikeDao commonProfileAerospikeDao;
	private final PromocodeAerospikeDao promocodeAerospikeDao;
	private final DependencyServicesCommunicator dependencyServicesCommunicator;
	private final JsonUtil jsonUtil;
	private final Tracker tracker;

	/**
	 * Utilities to process Promocode Promocode data.
	 */
	@Inject
	public PromocodeManager(DependencyServicesCommunicator dependencyServicesCommunicator,
			PromocodeAerospikeDao promocodeAerospikeDao, JsonUtil jsonUtil, Tracker tracker, CommonProfileAerospikeDao commonProfileAerospikeDao) {
		this.promocodeAerospikeDao = promocodeAerospikeDao;
		this.jsonUtil = jsonUtil;
		this.tracker = tracker;
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.commonProfileAerospikeDao = commonProfileAerospikeDao;
	}

	public UserPromocode usePromocodeForUser(String code, ClientInfo clientInfo) {
		Map.Entry<UserPromocode, PromoCodeEvent> promoCodeEntry = promocodeAerospikeDao.usePromocodeForUser(code, clientInfo.getUserId(), clientInfo.getAppId());
		tracker.addEvent(clientInfo, promoCodeEntry.getValue());

		return promoCodeEntry.getKey();
	}

	public UserPromocode getUserPromocodeById(String id) {
		String rawJson = promocodeAerospikeDao.getPromocodeById(id);
		return jsonUtil.fromJson(rawJson, UserPromocode.class);
	}

	public void updateUserPromocode(UserPromocode userPromocode) {
		promocodeAerospikeDao.updateUserPromocode(userPromocode);
	}

	public void transferPromocodeAmounts(JsonMessage<? extends JsonMessageContent> message,
										 SessionWrapper sessionWrapper, UserPromocode promocode) {
		final String userId = message.getClientInfo().getUserId();
		final PaymentEvent paymentEvent = new PaymentEvent();

		if (promocode.getBonuspointsValue() != null && promocode.getBonuspointsValue() > 0) {
			dependencyServicesCommunicator.initiateUserPromocodePayment(message, sessionWrapper, userId,
					promocode.getId(), BigDecimal.valueOf(promocode.getBonuspointsValue()), Currency.BONUS);
			paymentEvent.setBonusPointsGiven(promocode.getBonuspointsValue().longValue());
		}

		if (promocode.getMoneyValue() != null && promocode.getMoneyValue().signum() > 0) {
			dependencyServicesCommunicator.initiateUserPromocodePayment(message, sessionWrapper, userId,
					promocode.getId(), promocode.getMoneyValue(), Currency.MONEY);
			paymentEvent.setMoneyGiven(promocode.getMoneyValue());
		}

		if (promocode.getCreditValue() != null && promocode.getCreditValue() > 0) {
			dependencyServicesCommunicator.initiateUserPromocodePayment(message, sessionWrapper, userId,
					promocode.getId(), BigDecimal.valueOf(promocode.getCreditValue()), Currency.CREDIT);
			paymentEvent.setCreditGiven(promocode.getCreditValue().longValue());
		}

		tracker.addEvent(message.getClientInfo(), paymentEvent);
	}

	public Profile searchProfilesWithEmail(String email){
		return  this.commonProfileAerospikeDao.findByIdentifierWithCleanup(ProfileIdentifierType.EMAIL, email);
	}

	public void creditPromocodeAmounts(String promocode,
									   String tenantId,
									   String profileId,
									   int amount,
									   Currency currency) {
		dependencyServicesCommunicator.creditUserPromocodePayment(promocode,
				 tenantId,
				 profileId,
				 amount,
				 currency
		);
	}
}
