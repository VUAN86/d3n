package de.ascendro.f4m.service.winning.model.component;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.winning.model.UserWinningComponent;
import de.ascendro.f4m.service.winning.model.UserWinningComponentStatus;
import de.ascendro.f4m.service.winning.model.WinningOption;

public class ApiUserWinningComponent implements JsonMessageContent {

	private String userWinningComponentId;
	private String winningComponentId;
	private String gameInstanceId;
	private String title;
	private String gameType;
	private String casinoComponent;
	private String type;
	private String status;
	private String assignTimestamp;
	private String useTimestamp;
	private String fileTimestamp;
	private String rules;
	private String description;
	private String imageId;
	private String sourceGameType;
	private String info;
	private ApiWinningOption winning;
	
	public ApiUserWinningComponent(UserWinningComponent userWinningComponent) {
		userWinningComponentId = userWinningComponent.getUserWinningComponentId();
		winningComponentId = userWinningComponent.getWinningComponentId();
		gameInstanceId = userWinningComponent.getGameInstanceId();
		title = userWinningComponent.getTitle();
		gameType = userWinningComponent.getGameType() == null ? null : userWinningComponent.getGameType().name();
		casinoComponent = userWinningComponent.getCasinoComponent() == null ? null : userWinningComponent.getCasinoComponent().name();
		type = userWinningComponent.getType() == null ? null : userWinningComponent.getType().name();
		UserWinningComponentStatus wcStatus = userWinningComponent.getStatus();
		status = wcStatus == null ? null : wcStatus.name();
		rules = userWinningComponent.getRules();
		description = userWinningComponent.getDescription();
		imageId = userWinningComponent.getImageId();
		sourceGameType = userWinningComponent.getSourceGameType() == null ? null : userWinningComponent.getSourceGameType().name();
		info = userWinningComponent.getInfo();
		assignTimestamp = userWinningComponent.getAssignTimestamp() == null ? null : userWinningComponent.getPropertyAsString(UserWinningComponent.PROPERTY_ASSIGN_TIMESTAMP);
		useTimestamp = userWinningComponent.getUseTimestamp() == null ? null : userWinningComponent.getPropertyAsString(UserWinningComponent.PROPERTY_USE_TIMESTAMP);
		fileTimestamp = userWinningComponent.getFileTimestamp() == null ? null : userWinningComponent.getPropertyAsString(UserWinningComponent.PROPERTY_FILE_TIMESTAMP);
		if (wcStatus == UserWinningComponentStatus.FILED) {
			WinningOption wcWinning = userWinningComponent.getWinning();
			winning = wcWinning == null ? null : new ApiWinningOption(wcWinning);
		}
	}
	
	public String getUserWinningComponentId() {
		return userWinningComponentId;
	}

	public String getWinningComponentId() {
		return winningComponentId;
	}

	public String getGameInstanceId() {
		return gameInstanceId;
	}

	public String getTitle() {
		return title;
	}

	public String getType() {
		return type;
	}
	
	public String getGameType() {
		return gameType;
	}
	
	public String getCasinoComponent() {
		return casinoComponent;
	}

	public String getStatus() {
		return status;
	}

	public String getAssignTimestamp() {
		return assignTimestamp;
	}
	
	public String getUseTimestamp() {
		return useTimestamp;
	}
	
	public String getFileTimestamp() {
		return fileTimestamp;
	}
	
	public String getRules() {
		return rules;
	}
	
	public String getDescription() {
		return description;
	}

	public String getImageId() {
		return imageId;
	}

	public String getSourceGameType() {
		return sourceGameType;
	}

	public String getInfo() {
		return info;
	}

	public ApiWinningOption getWinning() {
		return winning;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("ApiUserWinningComponent [");
		builder.append("userWinningComponentId=").append(userWinningComponentId);
		builder.append("winningComponentId=").append(winningComponentId);
		builder.append(", gameInstanceId=").append(gameInstanceId);
		builder.append(", title=").append(title);
		builder.append(", type=").append(type);
		builder.append(", gameType=").append(gameType);
		builder.append(", casinoComponent=").append(casinoComponent);
		builder.append(", status=").append(status);
		builder.append(", assignTimestamp=").append(assignTimestamp);
		builder.append(", useTimestamp=").append(useTimestamp);
		builder.append(", fileTimestamp=").append(fileTimestamp);
		builder.append(", rules=").append(rules);
		builder.append(", imageId=").append(imageId);
		builder.append(", sourceGameType=").append(sourceGameType);
		builder.append(", info=").append(info);
		builder.append(", winning=").append(winning);
		builder.append("]");
		return builder.toString();
	}

}
