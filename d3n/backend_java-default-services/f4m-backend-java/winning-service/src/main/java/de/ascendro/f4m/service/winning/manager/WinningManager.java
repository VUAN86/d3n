package de.ascendro.f4m.service.winning.manager;

import com.google.gson.JsonObject;
import de.ascendro.f4m.server.analytics.model.InvoiceEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageError;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.random.RandomUtil;
import de.ascendro.f4m.service.winning.client.PaymentServiceCommunicator;
import de.ascendro.f4m.service.winning.client.ResultEngineCommunicator;
import de.ascendro.f4m.service.winning.dao.SuperPrizeAerospikeDao;
import de.ascendro.f4m.service.winning.exception.F4MComponentNotAvailableException;
import de.ascendro.f4m.service.winning.exception.WinningServiceExceptionCodes;
import de.ascendro.f4m.service.winning.model.*;
import de.ascendro.f4m.service.winning.model.component.UserWinningComponentAssignRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class WinningManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(WinningManager.class);

	private final PaymentServiceCommunicator paymentServiceCommunicator;
	private final ResultEngineCommunicator resultEngineCommunicator;
	private final WinningComponentManager winningComponentManager;
	private final CommonUserWinningAerospikeDao userWinningDao;
	private final InsuranceInterfaceWrapper insuranceApi;
	private final SuperPrizeAerospikeDao superPrizeDao;
	private final RandomUtil randomUtil;
	private final Tracker tracker;

	@Inject
	public WinningManager(PaymentServiceCommunicator paymentServiceCommunicator, ResultEngineCommunicator resultEngineCommunicator,
						  WinningComponentManager winningComponentManager, CommonUserWinningAerospikeDao userWinningDao, InsuranceInterfaceWrapper insuranceApi,
						  SuperPrizeAerospikeDao superPrizeDao, RandomUtil randomUtil, Tracker tracker)
	{
		this.paymentServiceCommunicator = paymentServiceCommunicator;
		this.resultEngineCommunicator = resultEngineCommunicator;
		this.winningComponentManager = winningComponentManager;
		this.userWinningDao = userWinningDao;
		this.insuranceApi = insuranceApi;
		this.superPrizeDao = superPrizeDao;
		this.randomUtil = randomUtil;
		this.tracker = tracker;
	}

	public UserWinning getUserWinning(String appId, String userWinningId) {
		return userWinningDao.getUserWinning(appId, userWinningId);
	}
	
	public List<JsonObject> getUserWinnings(String appId, String userId, int limit, long offset, List<OrderBy> orderBy) {
		return userWinningDao.getUserWinnings(appId, userId, limit, offset, orderBy);
	}

	/**
	 * If freeOrPaid is true, free options are used (credit, bonus, voucher), otherwise paid options are used (money, super)
	 *
	 * @param userId
	 * @param winningComponent
	 * @param freeOrPaid
	 * @return
	 */
	public WinningOption determineWinning(String userId, WinningComponent winningComponent, boolean freeOrPaid) {
		List<WinningOptionType> winningOptionTypes;
		if(freeOrPaid) {
			winningOptionTypes = Arrays.asList(WinningOptionType.BONUS, WinningOptionType.CREDITS, WinningOptionType.VOUCHER);
		} else {
			winningOptionTypes = Arrays.asList(WinningOptionType.SUPER, WinningOptionType.MONEY);
		}
		return determineWinning(userId, winningComponent, winningOptionTypes);
	}

	public WinningOption determineWinning(String userId, WinningComponent winningComponent) {
		return determineWinning(userId, winningComponent, Arrays.asList(WinningOptionType.values()));
	}

	private WinningOption determineWinning(String userId, WinningComponent winningComponent, List<WinningOptionType> winningOptionTypes) {

		List<WinningOption> winningOptions = winningComponent.getWinningOptions().stream().filter(o -> winningOptionTypes.contains(o.getType())).collect(Collectors.toList());

		List<WinningOption> superPrizes = new ArrayList<>(1);
		List<WinningOption> normalPrizes = new ArrayList<>(winningComponent.getWinningOptions().size());
		winningOptions.forEach(winningOption -> {
			if (winningOption.isSuperPrize()) {
				superPrizes.add(winningOption);
			} else {
				normalPrizes.add(winningOption);
			}
		});

		if(winningOptionTypes.contains(WinningOptionType.SUPER)) {
			// First try to win super prize
			for (WinningOption superPrize : superPrizes) {
				try {
					if (isSuperPrizeWon(userId, superPrize.getPrizeId())) {
						return superPrize;
					}
				} catch (F4MEntryNotFoundException e) {
					LOGGER.error("Could not read super prize configuration", e);
					// but still continue - user is not to blame for our mistakes
				}
			}
		}
		// Then, see if normal prizes are won.
		normalPrizes.sort((winningOption1, winningOption2) -> 
				winningOption1.getPrizeIndex() == null ? -1 : winningOption1.getPrizeIndex().compareTo(winningOption2.getPrizeIndex()));
		return determineNormalWinning(winningComponent.getWinningComponentId(), normalPrizes);
	}
	
	private boolean isSuperPrizeWon(String userId, String superPrizeId) throws F4MEntryNotFoundException {
		SuperPrize superPrize = superPrizeDao.getSuperPrize(superPrizeId);
		if (superPrize != null) {
			int random = randomUtil.nextBlumBlumShubInt(superPrize.getRandomRangeFrom(), superPrize.getRandomRangeTo() + 1);
			return insuranceApi.callInsuranceApi(superPrize.getInsuranceUrl(userId, random));
		} else {
			LOGGER.error("Super prize with id {} not found", superPrizeId);
			return false;
		}
	}

	public void assignWinningComponentPayment(String winningComponentId, Game game, JsonMessage<UserWinningComponentAssignRequest> sourceMessage,
											  SessionWrapper sourceSession, boolean isEligibleToWinnings, String gameInstanceId)
	{
		GameWinningComponentListItem component = game.getWinningComponent(winningComponentId);
		if (component != null) {
			if (component.isPaid() && component.getAmount().signum() > 0) {
				paymentServiceCommunicator.requestAssignWinningComponentPayment(sourceMessage, sourceSession, game.getGameId(), gameInstanceId, component,
																				isEligibleToWinnings, true);
			} else {
				WinningComponent winningComponent = winningComponentManager.getWinningComponent(sourceMessage.getClientInfo().getTenantId(), winningComponentId);

				onWinningComponentAssignGotTransferBetweenAccountsResponse(false, sourceMessage, sourceSession, winningComponent, component,
																		   isEligibleToWinnings, true, null);
			}
		} else {
			component = game.getWinningComponent(winningComponentId);
			if (component.isPaid())
				paymentServiceCommunicator.requestAssignWinningComponentPayment(sourceMessage, sourceSession, game.getGameId(),
																				gameInstanceId, component, isEligibleToWinnings, false);

			throw new F4MComponentNotAvailableException(WinningServiceExceptionCodes.ERR_THE_REQUESTED_WINNING_COMPONENT_NOT_AVAILABLE);
		}
	}

	public void onWinningComponentAssignGotTransferBetweenAccountsResponse(boolean isPaidComponent,	JsonMessage<UserWinningComponentAssignRequest> sourceMessage,
																			SessionWrapper sourceSession, WinningComponent winningComponent,
																			GameWinningComponentListItem winningComponentConfiguration,
																			boolean isEligibleToWinnings, boolean isEligibleToComponent, JsonMessageError error)
	{
		if (error == null)
		{
			final UserWinningComponent userWinningComponent;
			if (isEligibleToWinnings && isEligibleToComponent)
			{
				userWinningComponent = assignWinningComponentToUser(sourceMessage, winningComponent, winningComponentConfiguration);
			}
			else userWinningComponent=null;

			//Add invoice event only for paid components
			if (isPaidComponent)
			{
				addInvoiceEvent(sourceMessage.getClientInfo(), winningComponentConfiguration, userWinningComponent);
			}
			resultEngineCommunicator.storeUserWinningComponentInResults(sourceMessage, sourceSession, userWinningComponent);
		}
	}

	private void addInvoiceEvent(ClientInfo clientInfo, GameWinningComponentListItem winningComponent, UserWinningComponent userWinningComponent)
	{
		InvoiceEvent invoiceEvent = new InvoiceEvent();
		invoiceEvent.setPaymentType(InvoiceEvent.PaymentType.WINNING);
		invoiceEvent.setPaymentAmount(winningComponent.getAmount());
		invoiceEvent.setCurrency(winningComponent.getCurrency());
		if (userWinningComponent != null && userWinningComponent.getSourceGameType() != null)
		{
			invoiceEvent.setGameType(userWinningComponent.getSourceGameType());
		}
		tracker.addEvent(clientInfo, invoiceEvent);
	}

	private UserWinningComponent assignWinningComponentToUser(JsonMessage<UserWinningComponentAssignRequest> sourceMessage,
															  WinningComponent winningComponent, GameWinningComponentListItem winningComponentConfiguration)
	{
		final String tenantId = sourceMessage.getClientInfo().getTenantId();
		final String appId = sourceMessage.getClientInfo().getAppId();
		final String userId = sourceMessage.getClientInfo().getUserId();
		final String gameInstanceId = sourceMessage.getContent().getGameInstanceId();
		return winningComponentManager.assignWinningComponentToUser(tenantId, appId, userId, gameInstanceId, winningComponent, winningComponentConfiguration);
	}

	public String getWinningComponentId(JsonMessage<UserWinningComponentAssignRequest> sourceMessage, String freeWinningComponentId, String paidWinningComponentId)
	{
		final WinningComponentType type = sourceMessage.getContent().getType();
		final String winningComponentId;
		if (WinningComponentType.FREE == type)
		{
			winningComponentId = freeWinningComponentId != null ? freeWinningComponentId : sourceMessage.getContent().getWinningComponentId();
		} else if (WinningComponentType.PAID == type)
		{
			winningComponentId = paidWinningComponentId != null ? paidWinningComponentId : sourceMessage.getContent().getWinningComponentId();
		} else {
			LOGGER.error("Unsupported winning component type [{}]", type);
			throw new F4MFatalErrorException("Unsupported winning component type");
		}
		return winningComponentId;
	}

	private WinningOption determineNormalWinning(String winningComponentId, List<WinningOption> winningOptions) {
		List<Integer> probabilities = winningOptions.stream().map(winningOption -> 
				winningOption.getProbability().setScale(2).movePointRight(2).intValue()).collect(Collectors.toList());
		int sumProbabilities = probabilities.stream().collect(Collectors.summingInt(n -> n));
		if (sumProbabilities != 10000) {
			// should be always 10000 (100.00 with decimal place moved to the right by 2)
			LOGGER.error("Probabilities are not set up correctly for winning component {}", winningComponentId);
		}
		// Choose the winning
		int randomNumber = randomUtil.nextInt(sumProbabilities); // this has to be evenly-distributed random number!
		int accruedProbability = 0;
		for (int i = 0 ; i < probabilities.size() ; i++){
			accruedProbability += probabilities.get(i);
			if (accruedProbability > randomNumber) {
				return winningOptions.get(i);
			}
		}
		LOGGER.error("Something went wrong with the prize calculation with randomNumber {} for winning component {}", 
				randomNumber, winningComponentId);
		return null;
	}

	public void moveWinnings(String sourceUserId, String targetUserId, Collection<String> appIds) {
		if (appIds != null) {
			for (String appId : appIds) {
				userWinningDao.moveWinnings(appId, sourceUserId, targetUserId);
			}
		}
	}

}
