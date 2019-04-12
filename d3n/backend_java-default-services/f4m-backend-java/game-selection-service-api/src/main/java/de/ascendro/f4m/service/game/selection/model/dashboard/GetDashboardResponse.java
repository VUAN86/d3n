package de.ascendro.f4m.service.game.selection.model.dashboard;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetDashboardResponse implements JsonMessageContent {
	
	@SerializedName(value = GetDashboardRequest.QUIZ_24)
	private ApiQuiz quiz;
	@SerializedName(value = GetDashboardRequest.DUEL)
	private ApiDuel duel;
	@SerializedName(value = GetDashboardRequest.TOURNAMENT)
	private ApiTournament tournament;
	@SerializedName(value = GetDashboardRequest.TIPP_TOURNAMENT)
	private ApiTournament tippTournament;
	@SerializedName(value = GetDashboardRequest.LIVE_TOURNAMENT)
	private ApiTournament liveTournament;
	@SerializedName(value = GetDashboardRequest.USER_TOURNAMENT)
	private ApiUserTournament userTournament;
	@SerializedName(value = GetDashboardRequest.USER_LIVE_TOURNAMENT)
	private ApiUserTournament userLiveTournament;
	@SerializedName(value = GetDashboardRequest.QUIZ_BATTLE)
	private ApiQuizBattle quizBattle;
	
	public GetDashboardResponse() {
		// initialize empty response
	}

	public ApiQuiz getQuiz() {
		return quiz;
	}

	public void setQuiz(ApiQuiz quiz) {
		this.quiz = quiz;
	}

	public ApiDuel getDuel() {
		return duel;
	}

	public void setDuel(ApiDuel duel) {
		this.duel = duel;
	}

	public ApiTournament getTournament() {
		return tournament;
	}

	public void setTournament(ApiTournament tournament) {
		this.tournament = tournament;
	}

	public ApiTournament getTippTournament() {
		return tippTournament;
	}

	public void setTippTournament(ApiTournament tippTournament) {
		this.tippTournament = tippTournament;
	}

	public ApiUserTournament getUserTournament() {
		return userTournament;
	}

	public void setUserTournament(ApiUserTournament userTournament) {
		this.userTournament = userTournament;
	}

	public ApiQuizBattle getQuizBattle() {
		return quizBattle;
	}

	public void setQuizBattle(ApiQuizBattle quizBattle) {
		this.quizBattle = quizBattle;
	}

	public ApiTournament getLiveTournament() {
		return liveTournament;
	}

	public void setLiveTournament(ApiTournament liveTournament) {
		this.liveTournament = liveTournament;
	}

	public ApiUserTournament getUserLiveTournament() {
		return userLiveTournament;
	}

	public void setUserLiveTournament(ApiUserTournament userLiveTournament) {
		this.userLiveTournament = userLiveTournament;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetDashboardResponse [quiz=");
		builder.append(quiz);
		builder.append(", duel=");
		builder.append(duel);
		builder.append(", tournament=");
		builder.append(tournament);
		builder.append(", liveTournament=");
		builder.append(liveTournament);
		builder.append(", userTournament=");
		builder.append(userTournament);
		builder.append(", userLiveTournament=");
		builder.append(userLiveTournament);
		builder.append(", quizBattle=");
		builder.append(quizBattle);
		builder.append("]");
		return builder.toString();
	}

}
