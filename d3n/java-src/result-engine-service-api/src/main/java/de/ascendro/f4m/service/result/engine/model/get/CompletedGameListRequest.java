package de.ascendro.f4m.service.result.engine.model.get;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

/** Request for getting game history list. */
public class CompletedGameListRequest extends FilterCriteria implements JsonMessageContent {

	@JsonRequiredNullable
	private List<GameType> gameTypes;
	
	@JsonRequiredNullable
	private String dateFrom;

    @JsonRequiredNullable
	private String dateTo;
    
    @JsonRequiredNullable
	@Deprecated
	private GameType gameType;

	/** Maximum allowed requested list limit. */
	public static final int MAX_LIST_LIMIT = 100;

	public CompletedGameListRequest() {
		this(MAX_LIST_LIMIT, 0, Collections.emptyList(), null, null);
	}

	public CompletedGameListRequest(long offset) {
		this(MAX_LIST_LIMIT, offset, Collections.emptyList(), null, null);
	}

	public CompletedGameListRequest(int limit, long offset) {
		this(limit, offset, Collections.emptyList(), null, null);
	}
	
	public CompletedGameListRequest(int limit, long offset, List<GameType> gameTypes, String dateFrom, String dateTo) {
		setLimit(limit);
		setOffset(offset);
		setGameTypes(gameTypes);
		setDateFrom(dateFrom);
        setDateTo(dateTo);
	}

	@Deprecated
	public CompletedGameListRequest(int limit, long offset, GameType gameType, String dateFrom, String dateTo) {
		setLimit(limit);
		setOffset(offset);
		setGameType(gameType);
		setDateFrom(dateFrom);
        setDateTo(dateTo);
	}
	
	public List<GameType> getGameTypes() {
		// TODO: return gameTypes directly when deprecated gameType is deleted #10408
		if (gameTypes == null && gameType != null) {
			setGameType(gameType);
		}
		return gameTypes;
	}

	public void setGameTypes(List<GameType> gameTypes) {
		this.gameTypes = gameTypes;
	}

	public String getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(String dateFrom) {
		this.dateFrom = dateFrom;
	}

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }
    
    @Deprecated
	public GameType getGameType() {
		GameType type = null;
		if (gameTypes != null) {
			type = gameTypes.get(gameTypes.size() - 1);
		}
		return type;
	}

	@Deprecated
	public void setGameType(GameType gameType) {
		if (gameTypes == null) {
			gameTypes = new ArrayList<>(1);
		}
		gameTypes.add(gameType);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CompletedGameListRequest [gameTypes=");
		builder.append(gameTypes);
		builder.append(", dateFrom=");
		builder.append(dateFrom);
		builder.append(", dateTo=");
		builder.append(dateTo);
		builder.append(", gameType=");
		builder.append(gameType);
		builder.append("]");
		return builder.toString();
	}
    
}
