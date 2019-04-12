package de.ascendro.f4m.service.game.engine.advertisement;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.ascendro.f4m.service.game.selection.model.game.AdvertisementFrequency;

public class AdvertisementUtilTest {

	@Test
	public void testAdvertisementManager(){
		assertEquals(1, AdvertisementUtil.getAdvertisementCountWithinGame(AdvertisementFrequency.BEFORE_GAME, -1, null));
		
		assertEquals(1, AdvertisementUtil.getAdvertisementCountWithinGame(AdvertisementFrequency.AFTER_GAME, -1, null));
		
		assertEquals(5, AdvertisementUtil.getAdvertisementCountWithinGame(AdvertisementFrequency.AFTER_EACH_QUESTION, 5, null));
		
		assertEquals(1, AdvertisementUtil.getAdvertisementCountWithinGame(AdvertisementFrequency.AFTER_EVERY_X_QUESTION, 1, 1));
		assertEquals(2, AdvertisementUtil.getAdvertisementCountWithinGame(AdvertisementFrequency.AFTER_EVERY_X_QUESTION, 2, 1));
		assertEquals(3, AdvertisementUtil.getAdvertisementCountWithinGame(AdvertisementFrequency.AFTER_EVERY_X_QUESTION, 3, 1));
		assertEquals(1, AdvertisementUtil.getAdvertisementCountWithinGame(AdvertisementFrequency.AFTER_EVERY_X_QUESTION, 3, 2));
		assertEquals(1, AdvertisementUtil.getAdvertisementCountWithinGame(AdvertisementFrequency.AFTER_EVERY_X_QUESTION, 3, 3));
		assertEquals(0, AdvertisementUtil.getAdvertisementCountWithinGame(AdvertisementFrequency.AFTER_EVERY_X_QUESTION, 3, 0));
	}

}
