package de.ascendro.f4m.server.advertisement;

import com.google.inject.Inject;
import de.ascendro.f4m.server.advertisement.config.AdvertisementConfig;
import de.ascendro.f4m.server.advertisement.dao.AdvertisementDao;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.util.random.RandomSequenceGenerator;
import de.ascendro.f4m.service.util.random.RandomUtil;

import java.util.stream.IntStream;

public class AdvertisementManager {
	private final Config config;
	private final RandomUtil randomUtil;
	private final Tracker tracker;
	private final AdvertisementDao advertisementDao;

	@Inject
	public AdvertisementManager(Tracker tracker, Config config, AdvertisementDao advertisementDao,
			RandomUtil randomUtil) {
		this.tracker = tracker;
		this.config = config;
		this.randomUtil = randomUtil;
		this.advertisementDao = advertisementDao;
	}

    public void markAdvertisementShown(ClientInfo clientInfo, String advertisementBlobKey, String gameId, String gameInstanceId) {
        if(advertisementBlobKey == null) {
            throw new F4MEntryNotFoundException("Advertisement blob key cannot be empty");
        }

        AdEvent adEvent = new AdEvent();
        if (gameId != null) {
            adEvent.setGameId(Long.valueOf(gameId));
        }
        adEvent.setBlobKey(advertisementBlobKey);
        tracker.addEvent(clientInfo, adEvent);
        advertisementDao.addAdvertisementShown(gameInstanceId,advertisementBlobKey);
    }

	public String[] getRandomAdvertisementBlobKeys(int count, long providerId) {
		final String[] advertisementBlobKeys;
		final Integer availableAdvertisementCount = advertisementDao.getAdvertisementCount(providerId);
		if (availableAdvertisementCount != null && availableAdvertisementCount > 0) {
			IntStream advertisementIndexStream = IntStream.range(0, count);
			if (availableAdvertisementCount <= count) {//no random
				//play all ads from 1 to available with repeat, e.g. 1, 2, 3, 1, 2
				advertisementIndexStream = advertisementIndexStream.map(i -> i % availableAdvertisementCount + 1);
			} else {//random sequence
				final RandomSequenceGenerator randomSequenceGenerator = new RandomSequenceGenerator(
						availableAdvertisementCount, randomUtil);
				//pick random advertisements without repeats, starting from 1
				advertisementIndexStream = advertisementIndexStream
						.map(i -> randomSequenceGenerator.nextIntWithAutoReset() + 1);
			}
			advertisementBlobKeys = advertisementIndexStream.mapToObj(i -> getBlobKey(providerId, i))
					.toArray(String[]::new);
		} else {
			advertisementBlobKeys = new String[0];
		}
		return advertisementBlobKeys;
	}

	public String getBlobKey(long providerId, long index) {
		return String.format(config.getProperty(AdvertisementConfig.ADVERTISEMENT_PATH_FORMAT), providerId, index);
	}
}
