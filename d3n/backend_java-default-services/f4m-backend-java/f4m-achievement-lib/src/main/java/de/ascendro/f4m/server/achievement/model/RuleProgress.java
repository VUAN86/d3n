package de.ascendro.f4m.server.achievement.model;

public class RuleProgress extends ExecutionRule {

	private Integer progress;

	public RuleProgress() {
	}


	public RuleProgress(ExecutionRule rule) {
		this.setProgress(0);
		this.setLevel(rule.getLevel());
		this.setRule(rule.getRule());
	}

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(Integer progress) {
		this.progress = progress;
	}
}
