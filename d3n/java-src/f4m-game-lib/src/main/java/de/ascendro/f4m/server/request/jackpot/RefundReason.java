package de.ascendro.f4m.server.request.jackpot;

public enum RefundReason
{
    NO_OPPONENT("noOpponent"),
    GAME_NOT_FINISHED("gameNotFinished"),
    GAME_WAS_A_PAT("gameWasAPat"),
    GAME_NOT_PLAYED("notPlayed"),
    RESULT_CALCULATION_FAILED("resultCalculationFailed"),
    BACKEND_FAILED("backendFailed"),
    OTHER("other");

    private String value;

    private RefundReason(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

