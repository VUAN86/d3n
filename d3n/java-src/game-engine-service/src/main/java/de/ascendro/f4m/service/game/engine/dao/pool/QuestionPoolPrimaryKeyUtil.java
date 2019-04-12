package de.ascendro.f4m.service.game.engine.dao.pool;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;

import de.ascendro.f4m.server.game.util.GamePrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.model.InternationalQuestionKey;
import de.ascendro.f4m.service.game.engine.model.QuestionKey;

public class QuestionPoolPrimaryKeyUtil extends GamePrimaryKeyUtil {
	
	/*Base: category:%s:type:%s:complexity:%d:..	 */
	private static final String BASE_PRIMARY_KEY_FORMAT = "category" + KEY_ITEM_SEPARATOR + "%s" + KEY_ITEM_SEPARATOR
			+ "type" + KEY_ITEM_SEPARATOR + "%s" + KEY_ITEM_SEPARATOR + "complexity" + KEY_ITEM_SEPARATOR + "%d";

	/*single meta: ..:language:%s:meta */
	private static final String QUESTION_POOL_META_KEY_FORMAT = BASE_PRIMARY_KEY_FORMAT + KEY_ITEM_SEPARATOR
			+ "language" + KEY_ITEM_SEPARATOR + "%s" + KEY_ITEM_SEPARATOR + "meta";
	/*single: ..:language:%s:%d */
	private static final String SINGLE_QUESTION_KEY_FORMAT = BASE_PRIMARY_KEY_FORMAT + KEY_ITEM_SEPARATOR + "language"
			+ KEY_ITEM_SEPARATOR + "%s" + KEY_ITEM_SEPARATOR + "%d";

	/*index: ..:index:%s:meta */
	private static final String QUESTION_INDEX_META_KEY_FORMAT =
			BASE_PRIMARY_KEY_FORMAT + KEY_ITEM_SEPARATOR + "index" + KEY_ITEM_SEPARATOR + "%s" + KEY_ITEM_SEPARATOR + "meta";
	/*index meta: ..:index:%s:%d */
	private static final String QUESTION_INDEX_KEY_FORMAT =
			BASE_PRIMARY_KEY_FORMAT + KEY_ITEM_SEPARATOR + "index" + KEY_ITEM_SEPARATOR + "%s" + KEY_ITEM_SEPARATOR + "%d";

	@Inject
	public QuestionPoolPrimaryKeyUtil(Config config) {
		super(config);
	}

	@Override
	public String createPrimaryKey(String id) {
		throw new UnsupportedOperationException("Simple primary key not supported for question pool set");
	}

	public String createSingleQuestionKey(QuestionKey questionKey, long index) {
		return String.format(SINGLE_QUESTION_KEY_FORMAT, questionKey.getPoolId(), questionKey.getType(),
				questionKey.getComplexity(), questionKey.getLanguage(), index);
	}

	public String createSingleQuestionMetaKey(QuestionKey questionKey) {
		return String.format(QUESTION_POOL_META_KEY_FORMAT, questionKey.getPoolId(), questionKey.getType(), questionKey.getComplexity(),
				questionKey.getLanguage());
	}

	public String createQuestionIndexMetaKey(InternationalQuestionKey interQuestionKey) {
		return String.format(QUESTION_INDEX_META_KEY_FORMAT, interQuestionKey.getPoolId(),
				interQuestionKey.getType(), interQuestionKey.getComplexity(),
				toLangaugeKey(interQuestionKey.getPlayingLanguages()));
	}

	public String createQuestionIndexKey(InternationalQuestionKey interQuestionKey, long index) {
		return String.format(QUESTION_INDEX_KEY_FORMAT, interQuestionKey.getPoolId(),
				interQuestionKey.getType(), interQuestionKey.getComplexity(),
				toLangaugeKey(interQuestionKey.getPlayingLanguages()), index);
	}
	
	private String toLangaugeKey(String[] languages){
		final String languageKey;
		if (ArrayUtils.isNotEmpty(languages)) {
			Arrays.sort(languages);
			languageKey = String.join("+", languages);
		} else {
			languageKey = null;
		}
		return languageKey;
	}

}
