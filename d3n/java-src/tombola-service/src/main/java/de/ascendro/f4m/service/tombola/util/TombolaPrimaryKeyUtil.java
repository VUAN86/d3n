package de.ascendro.f4m.service.tombola.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.tombola.TombolaMessageTypes;

/**
 * Tombola service aerospike key construct helper
 *
 */
public class TombolaPrimaryKeyUtil extends PrimaryKeyUtil<String> {


	private static final String KEY_ITEM_APP_ID = "appId";
	private static final String KEY_ITEM_DRAW_DATE = "drawDate";
	private static final String KEY_ITEM_PROFILE = "profile";
	private static final String KEY_ITEM_DRAW_MONTH = "drawMonth";
	private static final String KEY_ITEM_DRAWN_MONTHS = "drawnMonths";
	private static final String KEY_ITEM_PENDING_DRAWING = "pendingDrawing";
	private static final String KEY_ITEM_HISTORY = "history";
	private static final String KEY_ITEM_TOMBOLA = "tombola";
	private static final String KEY_ITEM_TENANT_ID = "tenantId";
	private static final String KEY_ITEM_TICKET = "ticket";
	private static final String KEY_ITEM_STATISTICS = "statistics";
	private static final String KEY_ITEM_TENANT_IDS = "tenantIds";
	private static final String KEY_ITEM_APP_IDS = "appIds";
	private static final String KEY_ITEM_BOOST = "boost";

	@Inject
	public TombolaPrimaryKeyUtil(Config config) {
		super(config);
	}

	/**
	 * Create tombola key by id.
	 * 
	 * @param tombolaId - the tombola Id
	 * @return key
	 */
	@Override
	public String createPrimaryKey(String tombolaId) {
		return TombolaMessageTypes.SERVICE_NAME + KEY_ITEM_SEPARATOR + tombolaId;
	}

	/**
	 * Create available tombola list key by appId.
	 *
	 * @param appId - the app Id
	 * @return key
	 */
	public String createAvailableTombolaListPrimaryKey(String appId) {
		return getAppIdKeySegment(appId);
	}

	/**
	 * Create tombola drawing list key by  appId and drawDate
	 * @param appId - the app Id
	 * @param drawDate - the draw date
	 * @return key
	 */
    public String createTombolaDrawingPrimaryKey(String appId, String drawDate) {
		return getAppIdKeySegment(appId) + KEY_ITEM_SEPARATOR +
				KEY_ITEM_DRAW_DATE + KEY_ITEM_SEPARATOR + drawDate;
    }

	private String getAppIdKeySegment(String appId) {
		return KEY_ITEM_APP_ID + KEY_ITEM_SEPARATOR + appId;
	}

    private String getProfileHistoryPrefix(String userId) {
        return getProfilePrefix(userId) + KEY_ITEM_SEPARATOR + KEY_ITEM_HISTORY;
    }

	private String getProfilePrefix(String userId) {
		return KEY_ITEM_PROFILE + KEY_ITEM_SEPARATOR + userId;
	}

	public String createUserTombolaPrimaryKey(String userId, String tombolaId) {
        return getProfilePrefix(userId) + KEY_ITEM_SEPARATOR + KEY_ITEM_TOMBOLA + KEY_ITEM_SEPARATOR + tombolaId;
    }
	/**
	 * Create userTombola history key by drawMonth
	 * @param userId - the user Id
	 * @param drawMonth - the month of the drawing, in format YYYY-MM
	 * @return key
	 */
	public String createUserTombolaHistoryByMonthPrimaryKey(String userId, String drawMonth) {
    	return getProfileHistoryPrefix(userId)  + KEY_ITEM_SEPARATOR
                + KEY_ITEM_DRAW_MONTH + KEY_ITEM_SEPARATOR + drawMonth;
	}

	/**
	 * Create userTombola history key for drawn Months list
	 * @param userId - the user Id
	 * @return key
	 */
	public String createUserTombolaHistoryDrawnMonthsListPrimaryKey(String userId) {
		return getProfileHistoryPrefix(userId)  + KEY_ITEM_SEPARATOR + KEY_ITEM_DRAWN_MONTHS;
	}

	/**
	 * Create userTombola history key by pendingDrawing flag
	 * @param userId - the user Id
	 * @return key
	 */
	public String createUserTombolaHistoryPendingDrawingPrimaryKey(String userId) {
		return getProfileHistoryPrefix(userId) + KEY_ITEM_SEPARATOR + KEY_ITEM_PENDING_DRAWING;
	}

	public String createSubRecordKeyByTombolaId(String tombolaId, String subRecordName) {
		final String baseRecordKey = createPrimaryKey(tombolaId);
		return createSubRecordPrimaryKey(baseRecordKey, subRecordName);
	}

	public String createBoostKeyByTombolaId(String tombolaId) {
		return KEY_ITEM_TOMBOLA + KEY_ITEM_SEPARATOR + tombolaId + KEY_ITEM_SEPARATOR + KEY_ITEM_BOOST;
	}

	public String createUserTombolaStatisticsByAppIdKey(String userId, String appId) {
    	return getProfilePrefix(userId) + KEY_ITEM_SEPARATOR + KEY_ITEM_APP_ID +
				KEY_ITEM_SEPARATOR + appId + KEY_ITEM_SEPARATOR + KEY_ITEM_STATISTICS;
	}

	public String createUserTombolaStatisticsByTenantIdKey(String userId, String tenantId) {
    	return getProfilePrefix(userId) + KEY_ITEM_SEPARATOR + KEY_ITEM_TENANT_ID +
				KEY_ITEM_SEPARATOR + tenantId + KEY_ITEM_SEPARATOR + KEY_ITEM_STATISTICS;
	}

	public String createUserTombolaStatisticsTenantIdsListKey(String userId) {
		return getProfilePrefix(userId) + KEY_ITEM_SEPARATOR + KEY_ITEM_STATISTICS + KEY_ITEM_SEPARATOR +
				KEY_ITEM_TENANT_IDS;
	}

	public String createUserTombolaStatisticsAppIdsListKey(String userId) {
		return getProfilePrefix(userId) + KEY_ITEM_SEPARATOR + KEY_ITEM_STATISTICS + KEY_ITEM_SEPARATOR +
				KEY_ITEM_APP_IDS;
	}

	public String createTombolaTicketKey(String tombolaId, String ticketid) {
	    return createPrimaryKey(tombolaId) + KEY_ITEM_SEPARATOR + KEY_ITEM_TICKET
                + KEY_ITEM_SEPARATOR + ticketid;
    }
}
