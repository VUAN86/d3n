package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4mFinishTimeTippTournamentGameNotExpired extends F4MClientException
{
    public F4mFinishTimeTippTournamentGameNotExpired(String message) {
        super(GameEngineExceptionCodes.F4M_FINISH_TIME_TIPP_TOURNAMENT_GAME_NOT_EXPIRED, message);
    }
}
