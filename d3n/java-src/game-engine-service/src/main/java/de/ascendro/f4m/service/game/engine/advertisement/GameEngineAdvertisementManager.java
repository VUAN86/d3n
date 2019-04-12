package de.ascendro.f4m.service.game.engine.advertisement;

import javax.inject.Inject;

import de.ascendro.f4m.server.advertisement.AdvertisementManager;
import de.ascendro.f4m.service.game.engine.dao.instance.GameInstanceAerospikeDao;
import de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency;
import de.ascendro.f4m.service.game.selection.model.game.Game;

public class GameEngineAdvertisementManager {

	private final GameInstanceAerospikeDao gameInstanceAerospikeDao;
	private final AdvertisementManager advertisementManager;
	
	@Inject
	public GameEngineAdvertisementManager(GameInstanceAerospikeDao gameInstanceAerospikeDao,
			AdvertisementManager advertisementManager) {
		this.gameInstanceAerospikeDao = gameInstanceAerospikeDao;
		this.advertisementManager = advertisementManager;
	}

	public String[] getAdvertisementBlobKeys(Game game, int numberOfQuestions){
		final String[] advertisementBlobKeys;
		if (game.hasAdvertisements()) {
			final AdvertisementFrequency advertisementFrequency = game.getAdvertisementFrequency();
			final Integer advFreqXQuestionDefinition = game.getAdvertisementFrequencyXQuestionDefinition();
			final Long advertisementProviderId = game.getAdvertisementProviderId();

			final int advertisementCountWithinGame = AdvertisementUtil.getAdvertisementCountWithinGame(advertisementFrequency,
					numberOfQuestions, advFreqXQuestionDefinition);
			advertisementBlobKeys = advertisementManager.getRandomAdvertisementBlobKeys(advertisementCountWithinGame,
					advertisementProviderId);
		} else {
			advertisementBlobKeys = null;
		}
		return advertisementBlobKeys;
	}

	public String getLoadingBlobKey(Game game){
		final String loadingBlobKey;
		if (game.hasLoadingScreen()) {
			final Long loadingProviderId = game.getLoadingScreenProviderId();
			String[] keys = advertisementManager.getRandomAdvertisementBlobKeys(1,loadingProviderId);
			if(keys.length>0){
				loadingBlobKey=keys[0];
			} else {
				loadingBlobKey=null;
			}
		} else {
			loadingBlobKey = null;
		}
		return loadingBlobKey;
	}

	public void markShowAdvertisementWasSent(String gameInstanceId) {
		gameInstanceAerospikeDao.markShowAdvertisementWasSent(gameInstanceId);
	}

}
