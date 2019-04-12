package de.ascendro.f4m.service.game.engine.dao.pool;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.exception.F4MQuestionCannotBeReadFromPool;
import de.ascendro.f4m.service.game.engine.model.InternationalQuestionKey;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionIndex;
import de.ascendro.f4m.service.game.engine.model.QuestionKey;

public class QuestionPoolAerospikeDaoImpl extends AerospikeDaoImpl<QuestionPoolPrimaryKeyUtil> implements QuestionPoolAerospikeDao {

	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionPoolAerospikeDaoImpl.class);

	public static final String QUESTION_BIN_NAME = "question";
	public static final String INDEX_BIN_NAME = "index";
	public static final String META_BIN_NAME = "meta";

	@Inject
	public QuestionPoolAerospikeDaoImpl(Config config, QuestionPoolPrimaryKeyUtil primaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, primaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}
	
	@Override
	public Question getQuestion(QuestionKey key, long index) {
		final String questionRecordKey = primaryKeyUtil.createSingleQuestionKey(key, index);
        LOGGER.debug("getQuestion key {} ", key);
		final String questionAsString = readJson(getSet(), questionRecordKey, QUESTION_BIN_NAME);
		return questionAsString != null ? jsonUtil.fromJson(questionAsString, Question.class) : null;
	}
	
	@Override
	public QuestionIndex getInternationalQuestionIndex(InternationalQuestionKey interQuestionKey, long index) {
		final String questionIndexRecordKey = primaryKeyUtil.createQuestionIndexKey(interQuestionKey, index);
		final String questionIndexAsString = readJson(getSet(), questionIndexRecordKey, INDEX_BIN_NAME);
		return questionIndexAsString != null ? jsonUtil.fromJson(questionIndexAsString, QuestionIndex.class) : null;
	}
	
	@Override
	public Question getQuestionByInternationlIndex(QuestionIndex questionIndex, String userLanguage) {
		final Long languageIndex = questionIndex.getIndex(userLanguage);
		if (languageIndex != null) {
			final QuestionKey singleQuestionKey = new QuestionKey(questionIndex.getPoolId(), questionIndex.getType(),
					questionIndex.getComplexity(), userLanguage);
			return getQuestion(singleQuestionKey, languageIndex);
		} else {
			throw new F4MQuestionCannotBeReadFromPool(
					String.format("International question language [%s] index is not found within QuestionIndex[%s]",
							userLanguage, questionIndex));
		}
	}

	protected long getTotalQuestionCount(QuestionKey questionKey) {
		return getMetaValue(primaryKeyUtil.createSingleQuestionMetaKey(questionKey));
	}
	
	protected long getTotalQuestionCount(InternationalQuestionKey interQuestionKey) {
		return getMetaValue(primaryKeyUtil.createQuestionIndexMetaKey(interQuestionKey));
	}
	
	private long getMetaValue(String recordMetaKey) {
		Long totalCount = null;
		try {
			totalCount = readLong(getSet(), recordMetaKey, META_BIN_NAME);
		} catch (AerospikeException aEx) {
			if (aEx.getResultCode() == ResultCode.KEY_NOT_FOUND_ERROR) {
				LOGGER.warn("Could not found question pool by key [{}]", recordMetaKey);
			} else {
				throw aEx;
			}
		}
		return Optional.ofNullable(totalCount).orElse(0L);
	}
	
	@Override
	public Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> getInternationalQuestionPoolSizes(String[] poolIds, String[] types, int[] complexities, 
			String[] playingLanguages) {
		final PoolSizeProvider poolSizeProvider = (poolId, type, complexity) 
				-> getTotalQuestionCount(new InternationalQuestionKey(poolId, type, complexity, playingLanguages));
		return getQuestionPoolSizes(poolIds, types, complexities, poolSizeProvider);
	}	
	
	@Override
	public Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> getQuestionPoolSizes(String[] poolIds, String[] types, int[] complexities,
			String userLanguage){
		final PoolSizeProvider poolSizeProvider = (poolId, type, complexity) 
				-> getTotalQuestionCount(new QuestionKey(poolId, type, complexity, userLanguage));
		return getQuestionPoolSizes(poolIds, types, complexities, poolSizeProvider);
	}

	/**
	 * Collect any question pool sizes for specified properties
	 * 
	 * @param poolIds
	 *            - pool ids
	 * @param types
	 *            - question types
	 * @param complexities
	 *            - question complexities
	 * @return map of question pool sizes
	 */
	private Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> getQuestionPoolSizes(String[] poolIds, String[] types, int[] complexities,
			PoolSizeProvider poolSizeProvider) {
		final Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> poolSizes = new HashMap<>();
		for (String type : types) {
			for (int complexity : complexities) {
				final QuestionPoolSizeIdentifier poolSizeIdentifier = new QuestionPoolSizeIdentifier(type, complexity);
				
				if(!poolSizes.containsKey(poolSizeIdentifier)){
					final QuestionPoolSizeMeta poolSizeMeta = new QuestionPoolSizeMeta(poolIds);
					for (String poolId : poolIds) {		
						final long poolSize = poolSizeProvider.getPoolSize(poolId, type, complexity);
						poolSizeMeta.addPoolSize(poolId, poolSize);
					}
					poolSizes.put(poolSizeIdentifier, poolSizeMeta);
				}
			}
		}
		
		return poolSizes;
	}

	protected String getSet() {
		return config.getProperty(GameEngineConfig.AEROSPIKE_QUESTION_POOL_SET);
	}
	
	@FunctionalInterface
	interface PoolSizeProvider {
		long getPoolSize(String poolId, String type, int complexity);
	}
}
