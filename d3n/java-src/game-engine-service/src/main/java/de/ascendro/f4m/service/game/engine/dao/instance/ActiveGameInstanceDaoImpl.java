package de.ascendro.f4m.service.game.engine.dao.instance;

import static de.ascendro.f4m.service.util.F4MEnumUtils.getEnum;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;

import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.query.PredExp;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.config.GameConfigImpl;
import de.ascendro.f4m.server.game.GameUtil;
import de.ascendro.f4m.server.game.util.GameEnginePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.model.ActiveGameInstance;
import de.ascendro.f4m.service.game.engine.model.GameInstance;
import de.ascendro.f4m.service.game.engine.model.GameState;
import de.ascendro.f4m.service.game.engine.model.GameStatus;
import de.ascendro.f4m.service.game.engine.model.end.GameEndStatus;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.payment.model.Currency;

public class ActiveGameInstanceDaoImpl  extends AerospikeDaoImpl<GameEnginePrimaryKeyUtil> implements ActiveGameInstanceDao {
	public static final String ID_BIN_NAME = "id";
	public static final String STATUS_BIN_NAME = "status";
	public static final String END_STATUS_BIN_NAME = "endStatus";
	public static final String ENTRY_FEE_CURRENCY_BIN_NAME = "entryFeeCcy";
	public static final String ENTRY_FEE_AMOUNT_BIN_NAME = "entryFeeAmount";
	public static final String USER_ID_BIN_NAME = "userId";
	public static final String INVITATION_EXPIRATION_TS_BIN_NAME = "inviteExpTs";
	public static final String GAME_PLAY_EXPIRATION_TS_BIN_NAME = "gamePlayExpTs";
	public static final String GAME_TYPE_BIN_NAME = "gameType";
	public static final String MGI_ID_BIN_NAME = "mgiId";
	public static final String TRAINING_MODE_BIN_NAME = "trainingMode";
	private static final String[] ALL_BINS = new String[] { ID_BIN_NAME, STATUS_BIN_NAME, END_STATUS_BIN_NAME,
			ENTRY_FEE_CURRENCY_BIN_NAME, ENTRY_FEE_AMOUNT_BIN_NAME, USER_ID_BIN_NAME, INVITATION_EXPIRATION_TS_BIN_NAME,
			GAME_PLAY_EXPIRATION_TS_BIN_NAME, GAME_TYPE_BIN_NAME, MGI_ID_BIN_NAME };
	
	@Inject
	public ActiveGameInstanceDaoImpl(Config config, GameEnginePrimaryKeyUtil primaryKeyUtil,
			AerospikeClientProvider aerospikeClientProvider, JsonUtil jsonUtil) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}

	@Override
	public void create(GameInstance gameInstance, ZonedDateTime invitationExpireDateTime) {
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstance.getId());
		
		final Currency entryFeeCurrency = gameInstance.getEntryFeeCurrency();
		final BigDecimal entryFeeAmount = gameInstance.getEntryFeeAmount();

		final GameState gameState = gameInstance.getGameState();
		final GameStatus status = gameState != null ? gameState.getGameStatus() : null;
		final GameEndStatus endStatus = gameState != null ? gameState.getGameEndStatus() : null;
		
		final long gamePlayExpirationTimestamp = GameUtil.getGamePlayExpirationTimestamp(
				config.getPropertyAsLong(GameConfigImpl.GAME_MAX_PLAY_TIME_IN_HOURS),
				gameInstance.getGame().getEndDateTime());
		final long invitationExpireTimestamp = invitationExpireDateTime != null
				? invitationExpireDateTime.toInstant().toEpochMilli() 
				: Long.MAX_VALUE;

		createRecord(getSet(), gameInstanceKey, 
				getStringBin(ID_BIN_NAME, gameInstance.getId()),
				getStringBin(STATUS_BIN_NAME, Optional.ofNullable(status).orElse(GameStatus.REGISTERED).name()), 
				getStringBin(END_STATUS_BIN_NAME, endStatus != null ? endStatus.name() : null),
				getStringBin(ENTRY_FEE_CURRENCY_BIN_NAME, entryFeeCurrency != null ? entryFeeCurrency.name() : null), 
				getStringBin(ENTRY_FEE_AMOUNT_BIN_NAME, bigDecimalToString(entryFeeAmount)),
				getStringBin(USER_ID_BIN_NAME, gameInstance.getUserId()),
				getLongBin(INVITATION_EXPIRATION_TS_BIN_NAME, invitationExpireTimestamp),
				getLongBin(GAME_PLAY_EXPIRATION_TS_BIN_NAME, gamePlayExpirationTimestamp),
				getStringBin(GAME_TYPE_BIN_NAME, gameInstance.getGame().getType().name()),
				getStringBin(MGI_ID_BIN_NAME, gameInstance.getMgiId()),
				getStringBin(TRAINING_MODE_BIN_NAME, booleanToString(gameInstance.isTrainingMode())));
	}
	
	protected String bigDecimalToString(BigDecimal decimal) {
		String result = null;
		if (decimal != null) {
			result = decimal.toPlainString();
		}
		return result;
	}
	protected String booleanToString(Boolean traniningMode){
		return traniningMode.toString();
	}

	@Override
	public void updateStatus(String gameInstanceId, GameStatus status, GameEndStatus endStatus) {
		final String gameInstanceKey = primaryKeyUtil.createPrimaryKey(gameInstanceId);
		
		updateString(getSet(), gameInstanceKey, STATUS_BIN_NAME, 
				(v, wp) -> Optional.ofNullable(status).orElse(GameStatus.REGISTERED).name());
		updateString(getSet(), gameInstanceKey, END_STATUS_BIN_NAME, 
				(v, wp) -> endStatus != null ? endStatus.name() : null);
	}
	
	@Override
	public void delete(String gameInstanceId) {
		deleteSilently(getSet(), primaryKeyUtil.createPrimaryKey(gameInstanceId));
	}
	
	public static ActiveGameInstance fromRecord(Record record) {
		final ActiveGameInstance activeGameInstance;
		if (record != null) {
			activeGameInstance = new ActiveGameInstance();
			activeGameInstance.setId(record.getString(ID_BIN_NAME));
			activeGameInstance.setMgiId(record.getString(MGI_ID_BIN_NAME));
			activeGameInstance.setUserId(record.getString(USER_ID_BIN_NAME));

			activeGameInstance.setStatus(getEnum(GameStatus.class, record.getString(STATUS_BIN_NAME)));
			activeGameInstance.setEndStatus(getEnum(GameEndStatus.class, record.getString(END_STATUS_BIN_NAME)));

			final String entryFeeAmount = record.getString(ENTRY_FEE_AMOUNT_BIN_NAME);
			if (entryFeeAmount != null) {
				activeGameInstance.setEntryFeeAmount(new BigDecimal(entryFeeAmount));
			}
			activeGameInstance.setEntryFeeCurrency(getEnum(Currency.class, record.getString(ENTRY_FEE_CURRENCY_BIN_NAME)));
			
			activeGameInstance.setInvitationExpirationTimestamp(record.getLong(INVITATION_EXPIRATION_TS_BIN_NAME));
			activeGameInstance.setGamePlayExpirationTimestamp(record.getLong(GAME_PLAY_EXPIRATION_TS_BIN_NAME));
			
			activeGameInstance.setGameType(getEnum(GameType.class, record.getString(GAME_TYPE_BIN_NAME)));
		} else {
			activeGameInstance = null;
		}
		return activeGameInstance;
	}
	
	@Override
	public void processActiveRecords(PredExp[] predExp, Consumer<ActiveGameInstance> entryCallback) {
		final Statement statement = new Statement();
		statement.setNamespace(getNamespace());
		statement.setSetName(getSet());
		statement.setBinNames(ALL_BINS);
		
		statement.setPredExp(predExp);
		
		try(final RecordSet resultSet = getAerospikeClient().query(null, statement)){
			while(resultSet.next()){
				entryCallback.accept(fromRecord(resultSet.getRecord()));
			}
		}
	}

	public ActiveGameInstance getActiveGameInstance(String gameInstanceId) {
		return fromRecord(getAerospikeClient().get(null, new Key(getNamespace(), getSet(), primaryKeyUtil.createPrimaryKey(gameInstanceId))));
	}

	@Override
	public boolean exists(String gameInstanceId) {
		return exists(getSet(), primaryKeyUtil.createPrimaryKey(gameInstanceId));
	}
	
	private String getSet(){
		return config.getProperty(GameEngineConfig.AEROSPIKE_ACTIVE_GAME_INSTANCE_SET);
	}
	
	@Override
	protected String getNamespace() {
		return config.getProperty(GameEngineConfig.AEROSPIKE_ACTIVE_GAME_INSTANCE_NAMESPACE);
	}
}
