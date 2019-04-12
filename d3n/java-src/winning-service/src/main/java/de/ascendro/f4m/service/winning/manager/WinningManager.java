package de.ascendro.f4m.service.winning.manager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import de.ascendro.f4m.server.winning.CommonUserWinningAerospikeDao;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.util.random.RandomUtil;
import de.ascendro.f4m.service.winning.dao.SuperPrizeAerospikeDao;
import de.ascendro.f4m.service.winning.model.SuperPrize;
import de.ascendro.f4m.service.winning.model.UserWinning;
import de.ascendro.f4m.service.winning.model.WinningComponent;
import de.ascendro.f4m.service.winning.model.WinningOption;
import de.ascendro.f4m.service.winning.model.WinningOptionType;

public class WinningManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(WinningManager.class);
	
	private final CommonUserWinningAerospikeDao userWinningDao;
	private final InsuranceInterfaceWrapper insuranceApi;
	private final SuperPrizeAerospikeDao superPrizeDao;
	private final RandomUtil randomUtil;
	
	@Inject
	public WinningManager(CommonUserWinningAerospikeDao userWinningDao, InsuranceInterfaceWrapper insuranceApi, 
			SuperPrizeAerospikeDao superPrizeDao, RandomUtil randomUtil) {
		this.userWinningDao = userWinningDao;
		this.insuranceApi = insuranceApi;
		this.superPrizeDao = superPrizeDao;
		this.randomUtil = randomUtil;
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
