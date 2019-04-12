package de.ascendro.f4m.service.result.engine.util;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.server.result.Results;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.HandicapRange;
import de.ascendro.f4m.service.game.selection.model.game.ResultConfiguration;

public class ResultEngineUtilImplTest {
	
	@InjectMocks
	private ResultEngineUtilImpl resultEngineUtil;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void calculateHandicapRangeTest() throws Exception {
		ResultConfiguration resultConfiguration = mock(ResultConfiguration.class);
		int rangeId0 = 1000;
		int rangeId1 = 1001;
		when(resultConfiguration.getTournamentHandicapStructure())
				.thenReturn(Arrays.asList(new HandicapRange(rangeId0, rangeId0, 50), new HandicapRange(rangeId1, 51, 100)));
		Game game = mock(Game.class);
		when(game.getResultConfiguration()).thenReturn(resultConfiguration);
		
		assertEquals(rangeId0, calculateHandicapRange(25, game).getHandicapRangeId());
		assertEquals(rangeId0, calculateHandicapRange(50, game).getHandicapRangeId());
		assertEquals(rangeId1, calculateHandicapRange(51, game).getHandicapRangeId());
		assertEquals(rangeId1, calculateHandicapRange(100, game).getHandicapRangeId());
		//fallback to first range, if user handical does not fall into any range
		assertEquals(rangeId0, calculateHandicapRange(101, game).getHandicapRangeId());
	}

	private Results calculateHandicapRange(int userHandicap, Game game) {
		Results results = new Results();
		results.setUserHandicap(userHandicap);
		resultEngineUtil.calculateHandicapRange(results, game);
		return results;
	}
}