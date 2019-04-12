package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.game.selection.model.game.GameWinningComponentListItem;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.winning.model.WinningComponent;
import de.ascendro.f4m.service.winning.model.WinningComponentType;
import de.ascendro.f4m.service.winning.model.WinningOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class ApiWinningComponent implements JsonMessageContent {

	private String winningComponentId;
	private String gameType;
	private String casinoComponent;
	private String type;
	private BigDecimal amount;
	private String currency;
	private Integer correctAnswerPercent;
	private String imageId;
	private String title;
	private String rules;
	private String description;
	private String info;
	private BigDecimal maxJackpotAmount;
	private String maxJackpotType;
	private List<ApiWinningOption> winningOptions;
	
	public ApiWinningComponent(WinningComponent winningComponent, GameWinningComponentListItem winningComponentConfiguration) {
		winningComponentId = winningComponent.getWinningComponentId();
		if (winningComponentConfiguration != null) {
			type = winningComponentConfiguration.isPaid() ? WinningComponentType.PAID.name() : WinningComponentType.FREE.name();
			amount = winningComponentConfiguration.getAmount();
			currency = winningComponentConfiguration.getCurrency() == null ? null : winningComponentConfiguration.getCurrency().name();
			correctAnswerPercent = winningComponentConfiguration.getRightAnswerPercentage();
		}
		gameType = winningComponent.getGameType() == null ? null : winningComponent.getGameType().name();
		casinoComponent = winningComponent.getCasinoComponent() == null ? null : winningComponent.getCasinoComponent().name();
		imageId = winningComponent.getImageId();
		title = winningComponent.getTitle();
		rules = winningComponent.getRules();
		description = winningComponent.getDescription();
		info = winningComponent.getInfo();
		maxJackpotAmount=winningComponent.getJackpotAmount();
		maxJackpotType=winningComponent.getJackpotType();
		List<WinningOption> options = winningComponent.getWinningOptions();
		winningOptions = options == null ? null : options.stream().map(ApiWinningOption::new)
				.collect(Collectors.toList());
	}
	
	public String getWinningComponentId() {
		return winningComponentId;
	}

	public String getGameType() {
		return gameType;
	}
	
	public String getCasinoComponent() {
		return casinoComponent;
	}
	
	public String getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getCurrency() {
		return currency;
	}

	public Integer getCorrectAnswerPercent() {
		return correctAnswerPercent;
	}
	
	public String getImageId() {
		return imageId;
	}

	public String getTitle() {
		return title;
	}

	public String getRules() {
		return rules;
	}

	public String getDescription() {
		return description;
	}
	
	public String getInfo() {
		return info;
	}

	public BigDecimal getMaxJackpotAmount() {
		return maxJackpotAmount;
	}

	public String getMaxJackpotType() {
		return maxJackpotType;
	}

	public List<ApiWinningOption> getWinningOptions() {
		return winningOptions;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiWinningComponent [");
		builder.append("winningComponentId=").append(winningComponentId);
		builder.append(", type=").append(type);
		builder.append(", amount=").append(amount);
		builder.append(", currency=").append(currency);
		builder.append(", correctAnswerPercent=").append(correctAnswerPercent);
		builder.append(", imageId=").append(imageId);
		builder.append(", title=").append(title);
		builder.append(", rules=").append(rules);
		builder.append(", description=").append(description);
		builder.append(", info=").append(info);
		builder.append(", winningOptions=").append(winningOptions);
		builder.append(", maxJackpotAmount=").append(maxJackpotAmount);
		builder.append(", maxJackpotType=").append(maxJackpotType);
		builder.append("]");
		return builder.toString();
	}

}
