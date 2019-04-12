package de.ascendro.f4m.service.game.engine.dao.pool;

import java.util.Map;

import de.ascendro.f4m.service.game.engine.model.InternationalQuestionKey;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionIndex;
import de.ascendro.f4m.service.game.engine.model.QuestionKey;

public interface QuestionPoolAerospikeDao {

	Question getQuestion(QuestionKey key, long index);
	
	QuestionIndex getInternationalQuestionIndex(InternationalQuestionKey interQuestionKey, long index);
	
	Question getQuestionByInternationlIndex(QuestionIndex index, String userLanguage);

	/**
	 * Collect international question pool sizes for specified properties
	 * 
	 * @param poolIds
	 *            - pool ids
	 * @param types
	 *            - question types
	 * @param complexities
	 *            - question complexities
	 * @param playingLanguages - game languages
	 * @return map of question pool sizes mapped to pool size identifier
	 */
	Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> getInternationalQuestionPoolSizes(String[] poolIds, String[] types,
			int[] complexities, String[] playingLanguages);

	/**
	 * Collect regular question pool sizes for specified properties
	 * 
	 * @param poolIds
	 *            - pool ids
	 * @param types
	 *            - question types
	 * @param complexities
	 *            - question complexities
	 * @param userLanguage 
	 *            - expected user language
	 * @return map of question pool sizes mapped to pool size identifier
	 */
	Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> getQuestionPoolSizes(String[] poolIds, String[] types, int[] complexities,
			String userLanguage);
	
}
