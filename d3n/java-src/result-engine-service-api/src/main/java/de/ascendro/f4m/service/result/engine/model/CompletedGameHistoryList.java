package de.ascendro.f4m.service.result.engine.model;

import java.util.ArrayList;
import java.util.List;

public class CompletedGameHistoryList {

	private long total;

	private List<CompletedGameHistoryInfo> completedGameList;

	public CompletedGameHistoryList() {
		this.total = 0L;
		this.completedGameList = new ArrayList<>();
	}

	public CompletedGameHistoryList(long total, List<CompletedGameHistoryInfo> completedGameList) {
		this.total = total;
		this.completedGameList = completedGameList;
	}

	public long getTotal() {
		return total;
	}
	
	public List<CompletedGameHistoryInfo> getCompletedGameList() {
		return completedGameList;
	}
	
	public void addCompletedGameList(CompletedGameHistoryList completedGameList) {
		this.total += completedGameList.getTotal();
		this.completedGameList.addAll(completedGameList.getCompletedGameList());
	}
}
