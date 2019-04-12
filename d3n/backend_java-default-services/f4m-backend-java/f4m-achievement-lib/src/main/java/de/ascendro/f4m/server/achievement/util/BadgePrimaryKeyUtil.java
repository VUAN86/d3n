package de.ascendro.f4m.server.achievement.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.server.achievement.model.BadgeType;
import de.ascendro.f4m.service.config.Config;

public class BadgePrimaryKeyUtil extends PrimaryKeyUtil<String> {

    private static final String BADGE_LIST_PK_PREFIX = "badgeList";
	private static final String BADGE_PK_PREFIX = "badge";

    @Inject
    public BadgePrimaryKeyUtil(Config config) {
        super(config);
    }

	@Override
	public String createPrimaryKey(String badgeId) {
		return BADGE_PK_PREFIX + KEY_ITEM_SEPARATOR + badgeId;
	}

    public String createListPrimaryKey(String tenantId, BadgeType badgeType) {
        return BADGE_LIST_PK_PREFIX + KEY_ITEM_SEPARATOR + tenantId + KEY_ITEM_SEPARATOR +
                badgeType.toString();
    }
}
