package de.ascendro.f4m.service.game.engine.exception;

import de.ascendro.f4m.service.exception.client.F4MClientException;

public class F4MFinishTippTournamentGameNotFound extends F4MClientException
{
    public F4MFinishTippTournamentGameNotFound(String message) {
        super(GameEngineExceptionCodes.F4M_FINISH_TIPP_TOURNAMENT_MGI_ID_NOT_FOUND, message);
    }
}
