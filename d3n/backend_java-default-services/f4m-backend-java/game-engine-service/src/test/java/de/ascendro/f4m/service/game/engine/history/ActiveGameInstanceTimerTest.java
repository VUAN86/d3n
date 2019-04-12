package de.ascendro.f4m.service.game.engine.history;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.TimerTask;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class ActiveGameInstanceTimerTest {

	@Mock
	private Config config;
	@Mock
	private ActiveGameTimerTask task;
	private ActiveGameTimer activeGameInstanceTimer;
	
	private final Calendar firstScheduleTimeInUTC = Calendar.getInstance();
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		firstScheduleTimeInUTC.setTimeZone(TimeZone.getTimeZone(DateTimeUtil.TIMEZONE));
		activeGameInstanceTimer = new ActiveGameTimer(config, task) {
			@Override
			public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
				firstScheduleTimeInUTC.setTime(firstTime);
			}
		};
	}

	@Test
	public void testScheduleActiveGameInstanceCleanUp() {
		final String utcTime = "18:05";
		when(config.getProperty(GameConfigImpl.GAME_CLEAN_UP_UTC_TIME)).thenReturn(utcTime);
		activeGameInstanceTimer.scheduleActiveGameCleanUp();
		assertEquals(18, firstScheduleTimeInUTC.get(Calendar.HOUR_OF_DAY));
		assertEquals(5, firstScheduleTimeInUTC.get(Calendar.MINUTE));

		final ZonedDateTime now = DateTimeUtil.getCurrentDateTime();
		assertEquals(now.getDayOfMonth(), firstScheduleTimeInUTC.get(Calendar.DAY_OF_MONTH));
		assertEquals(now.getMonthValue() - 1, firstScheduleTimeInUTC.get(Calendar.MONTH));
		assertEquals(now.getYear(), firstScheduleTimeInUTC.get(Calendar.YEAR));
	}

}
