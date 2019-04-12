package de.ascendro.f4m.service.achievement.model.get;

import de.ascendro.f4m.server.achievement.model.Badge;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class BadgeGetResponse implements JsonMessageContent {
    private Badge badge;

    public BadgeGetResponse(Badge badge) {
        this.badge = badge;
    }

    public Badge getBadge() {
        return badge;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BadgeGetResponse [");
		builder.append("badge=").append(badge);
		builder.append("]");
		return builder.toString();
	}
}
