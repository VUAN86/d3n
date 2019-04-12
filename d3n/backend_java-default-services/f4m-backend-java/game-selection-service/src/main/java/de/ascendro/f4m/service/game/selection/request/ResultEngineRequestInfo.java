package de.ascendro.f4m.service.game.selection.request;

import de.ascendro.f4m.service.game.selection.model.dashboard.GetDashboardResponse;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

public class ResultEngineRequestInfo extends RequestInfoImpl {

	private final GetDashboardResponse dashboard;

	public ResultEngineRequestInfo(JsonMessage<? extends JsonMessageContent> sourceMessage,
			SessionWrapper sourceSession, GetDashboardResponse dashboard) {
		super(sourceMessage, sourceSession);
		this.dashboard = dashboard;
	}

	public GetDashboardResponse getDashboard() {
		return dashboard;
	}

}
