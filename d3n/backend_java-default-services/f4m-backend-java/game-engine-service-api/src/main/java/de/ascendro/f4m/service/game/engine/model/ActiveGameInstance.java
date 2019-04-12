package de.ascendro.f4m.service.game.engine.model;

import java.math.BigDecimal;

import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;

/**
 * Active Game instance reference
 */
public class ActiveGameInstance implements EntryFee {
	private String id;
	private GameStatus status;
	private GameEndStatus endStatus;

	private Currency entryFeeCurrency;
	private BigDecimal entryFeeAmount;
	
	private long invitationExpirationTimestamp;
	private long gamePlayExpirationTimestamp; 
	
	private GameType gameType;
	
	private String userId;
	private String mgiId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public GameStatus getStatus() {
		return status;
	}

	public void setStatus(GameStatus status) {
		this.status = status;
	}

	public GameEndStatus getEndStatus() {
		return endStatus;
	}

	public void setEndStatus(GameEndStatus endStatus) {
		this.endStatus = endStatus;
	}

	@Override
	public Currency getEntryFeeCurrency() {
		return entryFeeCurrency;
	}

	public void setEntryFeeCurrency(Currency entryFeeCurrency) {
		this.entryFeeCurrency = entryFeeCurrency;
	}

	@Override
	public BigDecimal getEntryFeeAmount() {
		return entryFeeAmount;
	}

	public void setEntryFeeAmount(BigDecimal entryFeeAmount) {
		this.entryFeeAmount = entryFeeAmount;
	}

	@Override
	public boolean isFree() {
		return entryFeeCurrency == null || entryFeeAmount == null || entryFeeAmount.compareTo(BigDecimal.ZERO) <= 0;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public long getInvitationExpirationTimestamp() {
		return invitationExpirationTimestamp;
	}

	public void setInvitationExpirationTimestamp(long invitationExpirationTimestamp) {
		this.invitationExpirationTimestamp = invitationExpirationTimestamp;
	}

	public long getGamePlayExpirationTimestamp() {
		return gamePlayExpirationTimestamp;
	}
	
	public void setGamePlayExpirationTimestamp(long gamePlayExpirationTimestamp) {
		this.gamePlayExpirationTimestamp = gamePlayExpirationTimestamp;
	}
	
	public GameType getGameType() {
		return gameType;
	}
	
	public void setGameType(GameType gameType) {
		this.gameType = gameType;
	}
	
	public boolean isMultiUserGame() {
		return gameType != null && gameType.isMultiUser();
	}
	
	public String getMgiId() {
		return mgiId;
	}
	
	public void setMgiId(String mgiId) {
		this.mgiId = mgiId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ActiveGameInstance [id=");
		builder.append(id);
		builder.append(", status=");
		builder.append(status);
		builder.append(", endStatus=");
		builder.append(endStatus);
		builder.append(", entryFeeCurrency=");
		builder.append(entryFeeCurrency);
		builder.append(", entryFeeAmount=");
		builder.append(entryFeeAmount);
		builder.append(", invitationExpirationTimestamp=");
		builder.append(invitationExpirationTimestamp);
		builder.append(", gamePlayExpirationTimestamp=");
		builder.append(gamePlayExpirationTimestamp);
		builder.append(", gameType=");
		builder.append(gameType);
		builder.append(", userId=");
		builder.append(userId);
		builder.append(", mgiId=");
		builder.append(mgiId);
		builder.append("]");
		return builder.toString();
	}
	
}
