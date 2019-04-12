package de.ascendro.f4m.service.game.selection.integration;

import static de.ascendro.f4m.server.game.GameAerospikeDao.BLOB_BIN_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDao;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.multiplayer.MultiplayerGameInstanceState;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDao;
import de.ascendro.f4m.server.multiplayer.dao.CommonMultiplayerGameInstanceDaoImpl;
import de.ascendro.f4m.server.multiplayer.dao.MultiplayerGameInstancePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfigBuilder;
import de.ascendro.f4m.service.game.selection.model.multiplayer.MultiplayerGameParameters;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;
import de.ascendro.f4m.service.util.ServiceUtil;

public class TestDataLoader extends JsonLoader{	
	public final static String SEPARATOR = "!";
	
	public static final String GAME_ID_0 = "game_id_0";
	public static final String GAME_ID_1 = "game_id_1";
	public static final String GAME_ID_2 = "game_id_2";
	public static final Integer DUEL_PLAYER_READINESS = 30;
	public static final Boolean DUEL_EMAIL_NOTIFICATION = false;
	public static final Integer TOURNAMENT_PLAYER_READINESS = 45;
	public static final Boolean TOURNAMENT_EMAIL_NOTIFICATION = true;
	public static final String GAME_INSTANCE_ID = "game_instance_id_1";
	public static final String MGI_ID = "mgi_id_1";
	public static final String TENANT_ID = "tenant_id_1";
	public static final String APP_ID = "app_id";
	public static final String START_MGI_TOPIC = MGI_ID;
	public static final String ANONYMOUS_CLIENT_ID = "b0fd1c1e-a718-11e6-80f5-76304dec7eb7";
	
	public static ZonedDateTime START_DATE_TIME = DateTimeUtil.getCurrentDateStart();
	public static ZonedDateTime END_DATE_TIME = DateTimeUtil.getCurrentDateEnd();
	
	private final Config config;
	private final AerospikeClientProvider aerospikeClientProvider;
	private final JsonUtil jsonUtil;
	private final ServiceUtil serviceUtil = new ServiceUtil();
	
	public TestDataLoader(Config config, AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil, JsonMessageUtil jsonMessageUtil) {
		super(new GameSelectionTest());
		this.config = config;
		this.aerospikeClientProvider = aerospikeClientProvider;
		this.jsonUtil = jsonUtil;
	}

	public static Map<String, String> loadTestData(InputStream in) throws IOException {
		Map<String, String> testData = new HashMap<>();

		String line;
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		while ((line = br.readLine()) != null) {
			int separatorIndex = line.indexOf(SEPARATOR);
			if (separatorIndex > 0 && separatorIndex < line.length()) {
				String key = line.substring(0, separatorIndex);
				String value = line.substring(separatorIndex + 1, line.length());

				testData.put(key, value);
			}
		}

		return testData;
	}
	
	/**
	 * Create multiplayer game instance by specific id and add additional to be added
	 * @param mgiId - mgi to used for create
	 * @param userId - creator id
	 * @param customGameConfig - custom game config to be created
	 * @param additionalInvitations - optional list of additional invitations
	 */
	public void inviteFriends(String mgiId, String userId, CustomGameConfigBuilder customGameConfigBuilder,
			List<Pair<String, String>> additionalInvitations) {
		final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao = getCommonMultiplayerGameInstanceDao(mgiId);
		commonMultiplayerGameInstanceDao.create(userId, customGameConfigBuilder.build(mgiId));
		
		if(!CollectionUtils.isEmpty(additionalInvitations)){
			additionalInvitations
				.forEach(p -> commonMultiplayerGameInstanceDao.addUser(mgiId, p.getKey(), p.getValue(), MultiplayerGameInstanceState.INVITED));
		}
		Assert.assertNotNull(commonMultiplayerGameInstanceDao.getConfig(mgiId));
	}
	
	public void registerUserForMultiplayerGame(String mgiId, String userId, String gameInstanceId){
		final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao = getCommonMultiplayerGameInstanceDao(mgiId);
		commonMultiplayerGameInstanceDao.registerForGame(mgiId, new ClientInfo("t1", "a1", userId, "ip", null), gameInstanceId);
	}
	
	private CommonMultiplayerGameInstanceDao getCommonMultiplayerGameInstanceDao(String mgiId) {
		final MultiplayerGameInstancePrimaryKeyUtil multiplayerGameInstancePrimaryKeyUtil = new MultiplayerGameInstancePrimaryKeyUtil(config){
			@Override
			public String generateId() {
				return mgiId;
			}
		};
		final CommonMultiplayerGameInstanceDao commonMultiplayerGameInstanceDao = new CommonMultiplayerGameInstanceDaoImpl(
				config, multiplayerGameInstancePrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
		return commonMultiplayerGameInstanceDao;
	}
	
	public String getReceiveGameStartNotificationsJson(String multiplayerGameInstanceId, boolean receive, ClientInfo clientInfo) throws IOException{
		 return getPlainTextJsonFromResources("receiveGameStartNotifications.json", clientInfo)
				.replace("{multiplayerGameInstanceId}", multiplayerGameInstanceId)
				.replace("\"{receive}\"", String.valueOf(receive));
				
	}
	
	public String getNotifySubscriberJson(String multiplayerGameInstanceId, String topic, Long time) throws IOException{
		String notifySubscriberJson = getPlainTextJsonFromResources("notifySubscriber.json")
				.replace("{topic}", topic)
				.replace("{multiplayerGameInstanceId}", multiplayerGameInstanceId);
		if(time != null){
			notifySubscriberJson = notifySubscriberJson.replace("{time}", time.toString());
		}else{
			notifySubscriberJson = notifySubscriberJson.replace("{time}", "0");
		}
		return notifySubscriberJson;
	}
	
	/**
	 * Json message for open registration event
	 * @param gameId - id of game config
	 * @param tenantId - game tenant id
	 * @param repititionAsIsoDateTime {@link de.ascendro.f4m.service.util.DateTimeUtil.JSON_DATETIME_FORMAT }
	 * @return complete JSON message
	 * @throws IOException
	 */
	public String getOpenRegistrationNotificationJson(String gameId, String tenantId, String repititionAsIsoDateTime) throws IOException {
		return getPlainTextJsonFromResources("openRegistrationNotification.json")
				.replace("{topic}", Game.getOpenRegistrationTopic(gameId))
				.replace("{gameId}", gameId)
				.replace("{tenantId}", tenantId)
				.replace("{repetition}", repititionAsIsoDateTime);
	}

	public String getOpenRegistrationNotificationLiveTournamentJson(String gameId, String tenantId,
			String repititionAsIsoDateTime, String startGameDateTime) throws IOException {
		return getPlainTextJsonFromResources("openRegistrationNotificationLiveTournament.json")
				.replace("{topic}", Game.getOpenRegistrationTopic(gameId)).replace("{gameId}", gameId)
				.replace("{tenantId}", tenantId).replace("{repetition}", repititionAsIsoDateTime)
				.replace("{startGameDateTime}", startGameDateTime);
	}	
	
	public String getStartGameNotificationJson(String gameId) throws IOException {
		return getPlainTextJsonFromResources("startGameNotification.json")
				.replace("{topic}", Game.getStartGameTopic(gameId))
				.replace("{gameId}", gameId);
	}
	
	public String getMergeProfileEventJson(String sourceUserId, String targetUserId) throws IOException {
		return getPlainTextJsonFromResources("mergeProfileEvent.json")
				.replace("{topic}", ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC)
				.replace("{sourceUserId}", sourceUserId)
				.replace("{targetUserId}", targetUserId);
	}
	
	public void updateGameTestData(AerospikeDao aerospikeDao) throws IOException {
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_GAME_SET);
		updateTestData("gameTestData.txt", set, aerospikeDao);
	}
	
	private void updateTestData(String fileName, String set, AerospikeDao aerospikeDao) throws IOException {
		try (InputStream in = this.getClass().getResourceAsStream(fileName)) {
			Map<String, String> testData = TestDataLoader.loadTestData(in);
			for (Entry<String, String> entry : testData.entrySet()) {
				String key = entry.getKey();
				String binValue = entry.getValue();

				aerospikeDao.deleteSilently(set, key);
				aerospikeDao.createJson(set, key, BLOB_BIN_NAME, binValue);
			}
		}
	}
	
	public String getInviteUsersToGameJson(String[] invitees, String mgiId) throws IOException{
		return getInviteUsersToGameJson(invitees, mgiId, null, null);
	}

	public String getInviteUsersToGameJson(String[] invitees, MultiplayerGameParameters mgParams)
			throws IOException{
		return getInviteUsersToGameJson(invitees, null, mgParams, null);
	}

	public String getInviteUsersToGameJson(String[] invitees, MultiplayerGameParameters mgParams, String[] pausedUsers)
			throws IOException{
		return getInviteUsersToGameJson(invitees, null, mgParams, pausedUsers);
	}
	
	private String getInviteUsersToGameJson(String[] invitees, String mgiId, MultiplayerGameParameters mgParams,
			String[] pausedUsers) throws IOException{
		return getPlainTextJsonFromResources("inviteUsersToGame.json", KeyStoreTestUtil.FULLY_REGISTERED_CLIENT_INFO)
				.replace("\"<<usersIds>>\"", jsonUtil.toJson(invitees))
				.replace("\"<<pausedUsersIds>>\"", pausedUsers != null ? jsonUtil.toJson(pausedUsers) : "null")
				.replace("\"<<multiplayerGameInstanceId>>\"", jsonUtil.toJson(mgiId))
				.replace("\"<<multiplayerGameParameters>>\"", jsonUtil.toJsonElement(mgParams).toString());

	}
	
	public String getRespondToInvitationJson(String mgiId, boolean accept) throws IOException{
		return getRespondToInvitationJson(mgiId, accept, KeyStoreTestUtil.FULLY_REGISTERED_CLIENT_INFO);
	}
	
	public String getRespondToInvitationJson(String mgiId, boolean accept, ClientInfo clientInfo) throws IOException{
		return getPlainTextJsonFromResources("respondToInvitation.json", clientInfo)
			.replace("<<multiplayerGameInstanceId>>", mgiId)
			.replace("\"<<accept>>\"", String.valueOf(accept))
			.replace("\"<<seq>>\"", String.valueOf(serviceUtil.generateId()));
	}
	
}
