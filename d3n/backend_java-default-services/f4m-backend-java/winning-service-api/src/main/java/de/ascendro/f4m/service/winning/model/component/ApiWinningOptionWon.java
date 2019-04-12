package de.ascendro.f4m.service.winning.model.component;

import java.math.BigDecimal;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.winning.model.WinningOption;

public class ApiWinningOptionWon implements JsonMessageContent {

	private String winningOptionId;
	private String prizeId;
	private String type;
	private BigDecimal amount;
	private String imageId;
	
	public ApiWinningOptionWon(WinningOption winningOption) {
		winningOptionId = winningOption.getWinningOptionId();
		prizeId = winningOption.getPrizeId();
		type = winningOption.getType() == null ? null : winningOption.getType().name();
		amount = winningOption.getAmount();
		imageId = winningOption.getImageId();
	}
	
	public String getWinningOptionId() {
		return winningOptionId;
	}

	public String getPrizeId() {
		return prizeId;
	}

	public String getType() {
		return type;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getImageId() {
		return imageId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiWinningOptionWon [");
		builder.append("winningOptionId=").append(winningOptionId);
		builder.append(", prizeId=").append(prizeId);
		builder.append(", type=").append(type);
		builder.append(", amount=").append(amount);
		builder.append(", imageId=").append(imageId);
		builder.append("]");
		return builder.toString();
	}
	
}
