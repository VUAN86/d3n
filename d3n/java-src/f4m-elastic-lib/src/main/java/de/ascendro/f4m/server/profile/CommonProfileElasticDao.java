package de.ascendro.f4m.server.profile;
import java.util.*;

import org.apache.commons.lang3.tuple.Pair;

import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.profile.model.Profile;

public interface CommonProfileElasticDao {

	void createOrUpdate(Profile profile);

	ListResult<Profile> searchProfiles(String appId, String searchTerm, String[] includeUserIds, String[] excludeUserIds, 
			PlayerListOrderType orderType, int limit, long offset);

			List<Profile> searchProfilesWithEmail(String email);

	/** Get rank of the given user among all players (left - rank, right - total player count). */
	Pair<Long, Long> getRank(String appId, String userId, Double handicap);
	
}
