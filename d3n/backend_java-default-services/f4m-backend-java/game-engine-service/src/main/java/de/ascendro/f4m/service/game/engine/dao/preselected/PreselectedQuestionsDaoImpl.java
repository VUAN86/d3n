package de.ascendro.f4m.service.game.engine.dao.preselected;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.config.GameEngineConfig;
import de.ascendro.f4m.service.game.engine.model.QuestionIndex;

public class PreselectedQuestionsDaoImpl extends AerospikeDaoImpl<PreselectedQuestionPrimaryKeyUtil> implements PreselectedQuestionsDao {
	private static final String QUESTIONS_BIN_NAME = "questions";
	private static final String CREATOR_ID_BIN_NAME = "creatorId";
	
	@Inject
	public PreselectedQuestionsDaoImpl(Config config, PreselectedQuestionPrimaryKeyUtil preselectedQuestionPrimaryKeyUtil, JsonUtil jsonUtil,
			AerospikeClientProvider aerospikeClientProvider) {
		super(config, preselectedQuestionPrimaryKeyUtil, jsonUtil, aerospikeClientProvider);
	}
	
	@Override
	public List<QuestionIndex> getQuestionIndexesByMgi(String mgiId) {
		final String mgiPrimaryKey = getPrimaryKeyUtil().createMgiPrimaryKey(mgiId);
		return readQuestionIndexes(mgiPrimaryKey);
	}
	
	@Override
	public void create(String mgiId, String creatorGameInstanceId, List<QuestionIndex> questionIndexes) {
		final String mgiPrimaryKey = getPrimaryKeyUtil().createMgiPrimaryKey(mgiId);
		createRecord(getSet(), mgiPrimaryKey, 
				getJsonBin(QUESTIONS_BIN_NAME, jsonUtil.toJson(questionIndexes)),
				getStringBin(CREATOR_ID_BIN_NAME, creatorGameInstanceId)
		);
	}
	
	@Override
	public String getCreatorId(String mgiId) {
		final String mgiPrimaryKey = getPrimaryKeyUtil().createMgiPrimaryKey(mgiId);
		return readString(getSet(), mgiPrimaryKey, CREATOR_ID_BIN_NAME);
	}
	
	private List<QuestionIndex> readQuestionIndexes(final String primaryKey) {
		final String questionsString = readJson(getSet(), primaryKey, QUESTIONS_BIN_NAME);
		final List<QuestionIndex> questionIndexes;
		if (!StringUtils.isEmpty(questionsString)) {
			questionIndexes = jsonUtil.fromJson(questionsString, new TypeToken<List<QuestionIndex>>(){}.getType());
		} else {
			questionIndexes = null;
		}
		return questionIndexes;
	}
	
	private PreselectedQuestionPrimaryKeyUtil getPrimaryKeyUtil(){
		return (PreselectedQuestionPrimaryKeyUtil)primaryKeyUtil;
	}
	
	protected String getSet() {
		return config.getProperty(GameEngineConfig.AEROSPIKE_PRESELECTED_QUESTIONS_SET);
	}
}
