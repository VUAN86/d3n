package de.ascendro.f4m.service.game.engine.dao.pool;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.model.InternationalQuestionKey;
import de.ascendro.f4m.service.game.engine.model.QuestionKey;

public class QuestionPoolPrimaryKeyUtilTest {

	@Mock
	private Config config;
	
	private QuestionPoolPrimaryKeyUtil questionPoolPrimaryKeyUtil;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		questionPoolPrimaryKeyUtil = new QuestionPoolPrimaryKeyUtil(config);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testCreatePrimaryKey() {
		final String anyId = UUID.randomUUID().toString();
		questionPoolPrimaryKeyUtil.createPrimaryKey(anyId);
	}

	@Test
	public void testCreateSingleQuestionKey() {
		final String singleQuestionKey = questionPoolPrimaryKeyUtil.createSingleQuestionKey(new QuestionKey("22", "35", 3, "en"), 1);
		assertEquals("category:22:type:35:complexity:3:language:en:1", singleQuestionKey);
	}

	@Test
	public void testCreateQuestionPoolMetaKey() {
		final String questionPoolMetaKey = questionPoolPrimaryKeyUtil.createSingleQuestionMetaKey(new QuestionKey("22", "35", 3, "en"));
		assertEquals("category:22:type:35:complexity:3:language:en:meta", questionPoolMetaKey);
	}

	@Test
	public void testCreateQuestionIndexMetaKey() {
		final InternationalQuestionKey internationalQuestionKey = new InternationalQuestionKey("22", "35", 3,
				new String[]{"en", "fr"});
		final String questionIndexMetaKey = questionPoolPrimaryKeyUtil.createQuestionIndexMetaKey(internationalQuestionKey);
		assertEquals("category:22:type:35:complexity:3:index:en+fr:meta", questionIndexMetaKey);
	}

	@Test
	public void testCreateQuestionIndexKey() {
		final InternationalQuestionKey internationalQuestionKey = new InternationalQuestionKey("22", "35", 3,
				new String[]{"en", "de", "fr"});
		final String questionIndexKey = questionPoolPrimaryKeyUtil.createQuestionIndexKey(internationalQuestionKey, 0);
		assertEquals("category:22:type:35:complexity:3:index:de+en+fr:0", questionIndexKey);
	}

}
