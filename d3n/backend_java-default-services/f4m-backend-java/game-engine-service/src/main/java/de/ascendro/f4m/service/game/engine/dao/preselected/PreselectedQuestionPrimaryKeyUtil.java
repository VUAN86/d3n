package de.ascendro.f4m.service.game.engine.dao.preselected;

import javax.inject.Inject;

import de.ascendro.f4m.server.game.util.GamePrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class PreselectedQuestionPrimaryKeyUtil extends GamePrimaryKeyUtil {
	private static final String MGI_PRIMARY_KEY_FORMAT = "gameEngine" + KEY_ITEM_SEPARATOR + "mgi:%s";

	@Inject
	public PreselectedQuestionPrimaryKeyUtil(Config config) {
		super(config);
	}

	@Override
	public String createPrimaryKey(String id) {
		throw new UnsupportedOperationException("Simple primary key not supported for question pool set");
	}

	public String createMgiPrimaryKey(String rsvpId) {
		return String.format(MGI_PRIMARY_KEY_FORMAT, rsvpId);
	}
}