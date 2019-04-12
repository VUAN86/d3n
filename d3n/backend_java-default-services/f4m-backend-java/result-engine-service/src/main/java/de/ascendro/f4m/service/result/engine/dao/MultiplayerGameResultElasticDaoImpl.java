package de.ascendro.f4m.service.result.engine.dao;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import de.ascendro.f4m.service.result.engine.util.ResultEngineUtilImpl;
import org.apache.commons.collections.CollectionUtils;

import de.ascendro.f4m.server.elastic.ElasticClient;
import de.ascendro.f4m.server.elastic.ElasticResponse;
import de.ascendro.f4m.server.elastic.query.BoolQuery;
import de.ascendro.f4m.server.elastic.query.Query;
import de.ascendro.f4m.server.elastic.query.RootQuery;
import de.ascendro.f4m.server.elastic.query.Sort;
import de.ascendro.f4m.server.elastic.query.SortDirection;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.result.engine.config.ResultEngineConfig;
import de.ascendro.f4m.service.result.engine.model.MultiplayerGameResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplayerGameResultElasticDaoImpl implements MultiplayerGameResultElasticDao {
	private static final Logger LOGGER = LoggerFactory.getLogger(MultiplayerGameResultElasticDaoImpl.class);

	private final ElasticClient elasticClient;
	private final ResultEngineConfig config;
	private boolean indexInitialized;
	
	@Inject
	public MultiplayerGameResultElasticDaoImpl(ElasticClient elasticClient, ResultEngineConfig config) {
		this.elasticClient = elasticClient;
		this.config = config;
	}

	@Override
	public void createOrUpdate(MultiplayerGameResult result) {
		initIndex();
		LOGGER.debug("MultiplayerGameResultElasticDaoImpl");
		elasticClient.createOrUpdate(getIndex(), getType(), result.getId(), result.getJsonObject());
	}

	@Override
	public ListResult<MultiplayerGameResult> listResults(String mgiId, Integer handicapRangeId,
			List<String> includedUserIds, int limit, long offset, boolean placeCalculated) {
		initIndex();
		boolean amongBuddies = CollectionUtils.isNotEmpty(includedUserIds);
		
		// Build query
		BoolQuery query = getResultListQuery(mgiId, handicapRangeId, includedUserIds, amongBuddies);
		// Build sort (by real place, if not searching among all ranges or among buddies)
		Sort sort = (placeCalculated && handicapRangeId != null && !amongBuddies)
				? Sort.newSort(MultiplayerGameResult.PROPERTY_PLACE)
				.sort(MultiplayerGameResult.PROPERTY_USER_ID)
				: Sort.newSort(MultiplayerGameResult.PROPERTY_CORRECT_ANSWERS, SortDirection.desc)
				.sort(MultiplayerGameResult.PROPERTY_GAME_POINTS_WITH_BONUS, SortDirection.desc)
				.sort(MultiplayerGameResult.PROPERTY_USER_ID);

		// Query
		ElasticResponse response = elasticClient.query(getIndex(), getType(), 
				RootQuery.query(query.build()).sort(sort).from(offset).size(limit));

		return new ListResult<>(limit, offset, response.getTotal(),
				response.getResults().stream().map(MultiplayerGameResult::new).collect(Collectors.toList()));
	}

	@Override
	public int getMultiplayerGameRankByResults(String multiplayerGameInstanceId, String userId, int correctAnswerCount,
			double gamePointsWithBonus, Integer handicapRangeId, List<String> buddyIds) {
		initIndex();		
		boolean amongBuddies = CollectionUtils.isNotEmpty(buddyIds);

		// Build query
		BoolQuery query = getResultListQuery(multiplayerGameInstanceId, handicapRangeId, buddyIds, amongBuddies);
		query.addMust(Query.bool()
				.addShould(Query.range(MultiplayerGameResult.PROPERTY_CORRECT_ANSWERS).gt(correctAnswerCount).build())
				.addShould(Query.bool()
						.addMust(Query.term(MultiplayerGameResult.PROPERTY_CORRECT_ANSWERS, correctAnswerCount))
						.addMust(Query.range(MultiplayerGameResult.PROPERTY_GAME_POINTS_WITH_BONUS).gt(gamePointsWithBonus).build())
						.build())
				.addShould(Query.bool()
						.addMust(Query.term(MultiplayerGameResult.PROPERTY_CORRECT_ANSWERS, correctAnswerCount))
						.addMust(Query.term(MultiplayerGameResult.PROPERTY_GAME_POINTS_WITH_BONUS, gamePointsWithBonus))
						.addMust(Query.range(MultiplayerGameResult.PROPERTY_USER_ID).lt(userId).build())
						.build())
				.build());
		ElasticResponse rank = elasticClient.query(getIndex(), getType(), RootQuery.query(query.build()).size(0));
		return (int) rank.getTotal() + 1;
	}

	private BoolQuery getResultListQuery(String mgiId, Integer handicapRangeId, List<String> buddyIds, boolean amongBuddies) {
		BoolQuery query = Query.bool()
				.addMust(Query.term(MultiplayerGameResult.PROPERTY_MULTIPLAYER_GAME_INSTANCE_ID, mgiId));
		if (handicapRangeId != null) {
			query.addMust(Query.term(MultiplayerGameResult.PROPERTY_HANDICAP_RANGE_ID, handicapRangeId));
		}
		if (amongBuddies) {
			query.addMust(Query.terms(MultiplayerGameResult.PROPERTY_USER_ID, buddyIds.toArray(new String[buddyIds.size()])));
		}
		return query;
	}

	private String getIndex() {
		return config.getProperty(ResultEngineConfig.ELASTIC_INDEX_MGR);
	}

	private String getType() {
		return config.getProperty(ResultEngineConfig.ELASTIC_TYPE_MGR);
	}
	
	private void initIndex() {
		if (! indexInitialized) {
			synchronized(this) {
				if (! indexInitialized) {
					String index = getIndex();
					if (! elasticClient.indexExists(index)) {
						elasticClient.createIndex(index, config.getProperty(ResultEngineConfig.ELASTIC_MAPPING_INDEX_MGR));
					}
					elasticClient.createMapping(index, config.getProperty(ResultEngineConfig.ELASTIC_TYPE_MGR), config.getProperty(ResultEngineConfig.ELASTIC_MAPPING_TYPE_MGR));
					indexInitialized = true;
				}
			}
		}
	}

}
