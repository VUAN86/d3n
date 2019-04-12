package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.util.F4MEnumUtils;
import de.ascendro.f4m.service.winning.model.WinningOption;
import de.ascendro.f4m.service.winning.model.WinningOptionType;

import java.math.BigDecimal;

public class ApiWinningOption implements JsonMessageContent {

	private String winningOptionId;
	private String prizeId;
	private Integer prizeIndex;
	private String type;
	private BigDecimal amount;
	private String imageId;
	private String title;

	public ApiWinningOption(WinningOption winningOption) {
		winningOptionId = winningOption.getWinningOptionId();
		prizeId = winningOption.getPrizeId();
		prizeIndex = winningOption.getPrizeIndex();
		type = winningOption.getType() == null ? null : winningOption.getType().name();
		amount = winningOption.getAmount();
		imageId = winningOption.getImageId();
		title = winningOption.getTitle();
	}
	
	public String getWinningOptionId() {
		return winningOptionId;
	}

	public String getPrizeId() {
		return prizeId;
	}

	public Integer getPrizeIndex() {
		return prizeIndex;
	}

	public WinningOptionType getType() {
		return F4MEnumUtils.getEnum(WinningOptionType.class, type);
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public String getImageId() {
		return imageId;
	}

	public String getTitle() {
		return title;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiWinningOption [");
		builder.append("winningOptionId=").append(winningOptionId);
		builder.append(", prizeId=").append(prizeId);
		builder.append(", prizeIndex=").append(prizeIndex);
		builder.append(", type=").append(type);
		builder.append(", amount=").append(amount);
		builder.append(", imageId=").append(imageId);
		builder.append(", title=").append(title);
		builder.append("]");
		return builder.toString();
	}
	
}
