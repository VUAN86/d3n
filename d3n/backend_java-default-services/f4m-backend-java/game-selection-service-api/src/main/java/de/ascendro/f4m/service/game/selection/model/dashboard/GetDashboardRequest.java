package de.ascendro.f4m.service.game.selection.model.dashboard;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetDashboardRequest implements JsonMessageContent {

	public static final String QUIZ_24 = "QUIZ24";
	public static final String DUEL = "DUEL";
	public static final String TOURNAMENT = "TOURNAMENT";
	public static final String TIPP_TOURNAMENT = "TIPP_TOURNAMENT";
	public static final String USER_TOURNAMENT = "USER_TOURNAMENT";
	public static final String LIVE_TOURNAMENT = "LIVE_TOURNAMENT";
	public static final String USER_LIVE_TOURNAMENT = "USER_LIVE_TOURNAMENT";
	public static final String QUIZ_BATTLE = "QUIZ_BATTLE";

	@SerializedName(value = QUIZ_24)
	private Boolean quiz;
	@SerializedName(value = DUEL)
	private Boolean duel;
	@SerializedName(value = TOURNAMENT)
	private Boolean tournament;
	@SerializedName(value = TIPP_TOURNAMENT)
	private Boolean tippTournament;
	@SerializedName(value = USER_TOURNAMENT)
	private Boolean userTournament;
	@SerializedName(value = LIVE_TOURNAMENT)
	private Boolean liveTournament;
	@SerializedName(value = USER_LIVE_TOURNAMENT)
	private Boolean userLiveTournament;
	@SerializedName(value = QUIZ_BATTLE)
	private Boolean quizBattle;

	public GetDashboardRequest() {
		// initialize empty request
	}

	public Boolean getQuiz() {
		return quiz;
	}

	public void setQuiz(Boolean quiz) {
		this.quiz = quiz;
	}

	public Boolean getDuel() {
		return duel;
	}

	public void setDuel(Boolean duel) {
		this.duel = duel;
	}

	public Boolean getTournament() {
		return tournament;
	}

	public void setTournament(Boolean tournament) {
		this.tournament = tournament;
	}

	public Boolean getTippTournament() {
		return tippTournament;
	}

	public void setTippTournament(Boolean tippTournament) {
		this.tippTournament = tippTournament;
	}

	public Boolean getUserTournament() {
		return userTournament;
	}

	public void setUserTournament(Boolean userTournament) {
		this.userTournament = userTournament;
	}

	public Boolean getQuizBattle() {
		return quizBattle;
	}

	public void setQuizBattle(Boolean quizBattle) {
		this.quizBattle = quizBattle;
	}

	public Boolean getLiveTournament() {
		return liveTournament;
	}

	public void setLiveTournament(Boolean liveTournament) {
		this.liveTournament = liveTournament;
	}

	public Boolean getUserLiveTournament() {
		return userLiveTournament;
	}

	public void setUserLiveTournament(Boolean userLiveTournament) {
		this.userLiveTournament = userLiveTournament;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetDashboardRequest [quiz=");
		builder.append(quiz);
		builder.append(", duel=");
		builder.append(duel);
		builder.append(", tournament=");
		builder.append(tournament);
		builder.append(", userTournament=");
		builder.append(userTournament);
		builder.append(", liveTournament=");
		builder.append(liveTournament);
		builder.append(", userLiveTournament=");
		builder.append(userLiveTournament);
		builder.append(", quizBattle=");
		builder.append(quizBattle);
		builder.append("]");
		return builder.toString();
	}
}