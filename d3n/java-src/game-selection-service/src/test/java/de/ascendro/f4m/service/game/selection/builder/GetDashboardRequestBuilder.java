package de.ascendro.f4m.service.game.selection.builder;

import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class GetDashboardRequestBuilder {

	private boolean quiz = false;
	private boolean duel = false;
	private boolean tournament = false;
	private boolean userTournament = false;
	private boolean liveTournament = false;
	private boolean quizBattle = false;
	private boolean userLiveTournament = false;
	
	private final JsonLoader jsonLoader = new JsonLoader(this);
	
	public static GetDashboardRequestBuilder create() {
		return new GetDashboardRequestBuilder();
	}
	
	public String build() throws Exception {
		return jsonLoader.getPlainTextJsonFromResources("getDashboardRequest.json", KeyStoreTestUtil.ANONYMOUS_CLIENT_INFO)
				.replaceFirst("\"<<quiz>>\"", Boolean.toString(quiz))
				.replaceFirst("\"<<duel>>\"", Boolean.toString(duel))
				.replaceFirst("\"<<tournament>>\"", Boolean.toString(tournament))
				.replaceFirst("\"<<userTournament>>\"", Boolean.toString(userTournament))
				.replaceFirst("\"<<liveTournament>>\"", Boolean.toString(liveTournament))
				.replaceFirst("\"<<userLiveTournament>>\"", Boolean.toString(userLiveTournament))
				.replaceFirst("\"<<quizBattle>>\"", Boolean.toString(quizBattle));
	}

	public GetDashboardRequestBuilder withQuiz(boolean quiz) {
		this.quiz = quiz;
		return this;
	}

	public GetDashboardRequestBuilder withDuel(boolean duel) {
		this.duel = duel;
		return this;
	}

	public GetDashboardRequestBuilder withTournament(boolean tournament) {
		this.tournament = tournament;
		return this;
	}

	public GetDashboardRequestBuilder withUserTournament(boolean userTournament) {
		this.userTournament = userTournament;
		return this;
	}

	public GetDashboardRequestBuilder withLiveTournament(boolean liveTournament) {
		this.liveTournament = liveTournament;
		return this;
	}

	public GetDashboardRequestBuilder withUserLiveTournament(boolean userLiveTournament) {
		this.userLiveTournament = userLiveTournament;
		return this;
	}

	public GetDashboardRequestBuilder withQuizBattle(boolean quizBattle) {
		this.quizBattle = quizBattle;
		return this;
	}

}
