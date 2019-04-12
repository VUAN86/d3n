package de.ascendro.f4m.service.analytics.module.achievement.updater;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import de.ascendro.f4m.service.analytics.module.achievement.processor.AchievementsLoader;
import de.ascendro.f4m.service.analytics.module.achievement.updater.AchievementsUpdaterScheduler;
import de.ascendro.f4m.service.util.DateTimeUtil;

@RunWith(MockitoJUnitRunner.class)
public class AchievementsUpdaterSchedulerTest {
	
	@Mock
	private AchievementsLoader loader;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testScheduling() {
		
		ScheduledExecutorService scheduler = Mockito.mock(ScheduledExecutorService.class);
		
		Answer<String> answer = new Answer<String>() {
			
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				long delay = invocation.getArgument(1);
				long period = invocation.getArgument(2);
				
				ZonedDateTime now = DateTimeUtil.getCurrentDateTime();
				ZonedDateTime next = now.plus(delay, ChronoUnit.MILLIS)
						.plus(1, ChronoUnit.SECONDS); // one more seconds because new time is computed later than original computation
				assertEquals(0, next.getHour());
				assertEquals(0, next.getMinute());
				assertEquals(24 * 60 * 60 * 1000, period);
				return null;
			}
		};
		
		Mockito.doAnswer(answer).when(scheduler).scheduleAtFixedRate(any(Runnable.class), any(Long.class), any(Long.class), eq(TimeUnit.MILLISECONDS));			
		
		AchievementsUpdaterScheduler service = spy(new AchievementsUpdaterScheduler(loader));
		when(service.getScheduler()).thenReturn(scheduler);
		
		service.schedule();
	}

}
