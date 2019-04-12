package de.ascendro.f4m.service.promocode.dao;

import java.util.Map;

import de.ascendro.f4m.server.AerospikeOperateDao;
import de.ascendro.f4m.server.analytics.model.PromoCodeEvent;
import de.ascendro.f4m.service.promocode.model.UserPromocode;

/**
 * Promocode Data Access Object interface.
 */
public interface PromocodeAerospikeDao extends AerospikeOperateDao {


    /**
     * Marks the promocode as used
     * @param userPromocodeId Full promcode ID, in the format 'promocode:instanceId'
	 * @param userId The ID of the user making the request
	 * @param appId The appId for the request
     */
	Map.Entry<UserPromocode, PromoCodeEvent> usePromocodeForUser(String userPromocodeId, String userId, String appId);

	String getPromocodeById(String id);

	void updateUserPromocode(UserPromocode userPromocode);

}
