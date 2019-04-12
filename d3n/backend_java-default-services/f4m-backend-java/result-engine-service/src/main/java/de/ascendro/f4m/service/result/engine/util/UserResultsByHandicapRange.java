package de.ascendro.f4m.service.result.engine.util;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import de.ascendro.f4m.server.result.RefundReason;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.HandicapRange;
import de.ascendro.f4m.service.payment.model.Currency;

public class UserResultsByHandicapRange {

	private int paidPlayerCount; //currently used only for testing purposes
	private int finishedPlayerCount; //currently used only for testing purposes
	private Map<HandicapRange, List<UserResults>> handicapRangePlacements = new HashMap<>();
	private Map<HandicapRange, Integer> handicapRangePaidPlayerCount = new HashMap<>();
	private Map<HandicapRange, Integer> handicapRangeFinishedPlayerCount = new HashMap<>();
	private Game game;
	private String tenantId;
	private String appId;
	private String multiplayerGameInstanceId;
	private BigDecimal totalEntryFeeAmount;
	private Currency entryFeeCurrency;
	private RefundReason refundReason;
	
	public UserResultsByHandicapRange(Game game, String tenantId, String appId, String multiplayerGameInstanceId,
			BigDecimal totalEntryFeeAmount, Currency entryFeeCurrency) {
		this.tenantId = tenantId;
		this.appId = appId;
		this.multiplayerGameInstanceId = multiplayerGameInstanceId; 
		this.totalEntryFeeAmount = totalEntryFeeAmount;
		this.entryFeeCurrency = entryFeeCurrency;
		this.game = game;
	}
	
	/**
	 * Add the game results.
	 */
	public void addHandicapRangeResults(HandicapRange range, List<UserResults> placements) {
		handicapRangePlacements.put(range, placements);
		final AtomicInteger finishedCount = new AtomicInteger(0);
		placements.forEach(r -> {
			if (r.isGameFinished()) {
				finishedCount.incrementAndGet();
			}
		});
		handicapRangePaidPlayerCount.put(range, placements.size());
		handicapRangeFinishedPlayerCount.put(range, finishedCount.get());
		
		paidPlayerCount += placements.size();
		finishedPlayerCount += finishedCount.get();
	}

	public void addDefaultHandicapRangeResults(List<UserResults> placements) {
		addHandicapRangeResults(HandicapRange.DEFAULT_HANDICAP_RANGE, placements);
	}
	
	/**
	 * Get number of handicap ranges.
	 */
	public int getHandicapRangeCount() {
		return handicapRangePlacements.size();
	}

	public List<UserResults> getPlacements(HandicapRange range) {
		return handicapRangePlacements.get(range);
	}
	
	public List<UserResults> getDefaultHandicapRangePlacements() {
		return getPlacements(HandicapRange.DEFAULT_HANDICAP_RANGE);
	}
	
	public int getPaidPlayerCount() {
		return paidPlayerCount;
	}
	
	public int getFinishedPlayerCount() {
		return finishedPlayerCount;
	}
	
	public int getPaidPlayerCount(HandicapRange range) {
		return handicapRangePaidPlayerCount.get(range);
	}
	
	public int getFinishedPlayerCount(HandicapRange range) {
		return handicapRangeFinishedPlayerCount.get(range);
	}

	public Game getGame() {
		return game;
	}

	public String getTenantId() {
		return tenantId;
	}

	public String getAppId() {
		return appId;
	}

	public String getMultiplayerGameInstanceId() {
		return multiplayerGameInstanceId;
	}
	
	public BigDecimal getTotalEntryFeeAmount() {
		return totalEntryFeeAmount;
	}
	
	public Currency getEntryFeeCurrency() {
		return entryFeeCurrency;
	}
	
	public RefundReason getRefundReason() {
		return refundReason;
	}
	
	public void setRefundReason(RefundReason refundReason) {
		this.refundReason = refundReason;
	}
	
	public void forEach(Consumer<UserResults> action) {
		handicapRangePlacements.values().stream().flatMap(List::stream).forEach(action);
	}
	
	public void forEachRange(Consumer<HandicapRange> action) {
		handicapRangePlacements.keySet().forEach(action);
	}
	
	public void forEach(HandicapRange range, Consumer<UserResults> action) {
		List<UserResults> placements = handicapRangePlacements.get(range);
		if (placements != null) {
			placements.forEach(action);
		}
	}

}
