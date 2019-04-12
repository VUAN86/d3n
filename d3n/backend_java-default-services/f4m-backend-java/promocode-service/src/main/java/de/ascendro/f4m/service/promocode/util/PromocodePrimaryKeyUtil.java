package de.ascendro.f4m.service.promocode.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.promocode.PromocodeMessageTypes;

/**
 * Profile service aerospike key construct helper
 *
 */
public class PromocodePrimaryKeyUtil extends PrimaryKeyUtil<String> {

	public static final String PROMOCODE_CAMPAIGN_KEY_PREFIX = "promocodeCampaign";
	public static final String NUMBER_OF_USES_COUNTER_KEY_PREFIX = "userPromoCount";


	@Inject
	public PromocodePrimaryKeyUtil(Config config) {
		super(config);
	}

	/**
	 * Create promocode key by id.
	 * 
	 * @param id
	 * @return key
	 */
	@Override
	public String createPrimaryKey(String id) {
		return PromocodeMessageTypes.SERVICE_NAME + KEY_ITEM_SEPARATOR + id;
	}

	public String createCampaignPrimaryKey(String promocodeCampaignId) {
		return PROMOCODE_CAMPAIGN_KEY_PREFIX + KEY_ITEM_SEPARATOR + promocodeCampaignId;
	}

	/**
	 * Create promocodeId sub-record key based on promocode and sub-record name.
	 *
	 * @param promocodeId
	 *            - profile user id
	 * @param subRecordName
	 *            - sub-record name
	 * @return sub-record key
	 */
	public String createSubRecordKeyByPromocodeId(String promocodeId, String subRecordName) {
		final String baseRecordKey = createPrimaryKey(promocodeId);
		return createSubRecordPrimaryKey(baseRecordKey, subRecordName);
	}

	/**
	 * Create userPromocode sub-record key based on userPromocodeId retrieved from API, which is in the form promocode:instanceId
	 *
	 * @param promocode
	 *            - promocode e.g SUPER10
	 * @return promocode record key
	 */
	public String createPromocodeKey(String promocode) {
		return PromocodeMessageTypes.SERVICE_NAME + KEY_ITEM_SEPARATOR + promocode;
	}

	public String createNumberOfUsesCounterKey(String code, String userId) {
		return NUMBER_OF_USES_COUNTER_KEY_PREFIX + KEY_ITEM_SEPARATOR + code + KEY_ITEM_SEPARATOR + userId;
	}
}
