package de.ascendro.f4m.service.game.selection.model.game.validate;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import de.ascendro.f4m.service.game.selection.exception.F4MPoolsNotValidException;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameParametersBase;

public class PoolsRule implements CustomParameterRule<GameParametersBase> {

	private static final String NOT_CONFIGURABLE = "Pools are not configurable; params [%s]";
	private static final String INVALID = "Pools in params must be subset of pools in Game config [%s]; params [%s]";

	@Override
	public void validate(GameParametersBase params, Game game) {
		if (ArrayUtils.isNotEmpty(params.getPoolIds())) {
			validatePoolIds(params, game);
		}
	}

	private void validatePoolIds(GameParametersBase params, Game game) {
		String[] gamePools = game.getAssignedPools();
		String[] configPools = params.getPoolIds();
		if (!game.getUserCanOverridePools()) {
			throw new F4MPoolsNotValidException(String.format(NOT_CONFIGURABLE, params));
		} else if (ArrayUtils.isEmpty(gamePools) || !Arrays.asList(gamePools).containsAll(Arrays.asList(configPools))) {
			throw new F4MPoolsNotValidException(String.format(INVALID, Arrays.toString(gamePools), params));
		}
	}

}
