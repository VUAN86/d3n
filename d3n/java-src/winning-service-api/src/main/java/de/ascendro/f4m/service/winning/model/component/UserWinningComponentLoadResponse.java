package de.ascendro.f4m.service.winning.model.component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.winning.model.WinningComponent;
import de.ascendro.f4m.service.winning.model.WinningOption;

public class UserWinningComponentLoadResponse implements JsonMessageContent {

	private List<ApiWinningOption> winningOptions;

	public UserWinningComponentLoadResponse(WinningComponent winningComponent) {
		List<WinningOption> options = winningComponent == null ? Collections.emptyList() : winningComponent.getWinningOptions();
		winningOptions = options.stream().map(ApiWinningOption::new).collect(Collectors.toList());
	}

	public List<ApiWinningOption> getWinningOptions() {
		return winningOptions;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("UserWinningComponentLoadResponse [");
		builder.append("winningOptions=").append(winningOptions);
		builder.append("]");
		return builder.toString();
	}

}
