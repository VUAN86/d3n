package de.ascendro.f4m.service.game.engine.server;

import static de.ascendro.f4m.service.game.engine.config.GameEngineConfig.QUESTION_PULL_NO_REPEATS_RETRY_COUNT;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.ASSIGNED_POOLS;
import static de.ascendro.f4m.service.game.engine.integration.TestDataLoader.MAX_COMPLEXITY;
import static de.ascendro.f4m.service.game.selection.model.game.QuestionOverwriteUsage.NO_REPEATS;
import static de.ascendro.f4m.service.game.selection.model.game.QuestionOverwriteUsage.REPEATS;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_ID;
import static de.ascendro.f4m.service.util.KeyStoreTestUtil.ANONYMOUS_USER_LANGUAGE;
import static org.apache.commons.lang3.ArrayUtils.toObject;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDao;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolSizeIdentifier;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolSizeMeta;
import de.ascendro.f4m.service.game.engine.exception.F4MQuestionCannotBeReadFromPool;
import de.ascendro.f4m.service.game.engine.exception.F4MQuestionsNotAvailableInPool;
import de.ascendro.f4m.service.game.engine.history.GameHistoryManager;
import de.ascendro.f4m.service.game.engine.integration.TestDataLoader;
import de.ascendro.f4m.service.game.engine.json.GameJsonBuilder;
import de.ascendro.f4m.service.game.engine.model.InternationalQuestionKey;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionBaseKey;
import de.ascendro.f4m.service.game.engine.model.QuestionIndex;
import de.ascendro.f4m.service.game.engine.model.QuestionKey;
import de.ascendro.f4m.service.game.engine.server.QuestionSelector.IsPlayedRule;
import de.ascendro.f4m.service.game.engine.server.QuestionSelector.PoolEntryProvider;
import de.ascendro.f4m.service.game.engine.server.QuestionSelector.SelectedPoolEntry;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.game.QuestionOverwriteUsage;
import de.ascendro.f4m.service.game.selection.model.game.QuestionTypeSpread;
import de.ascendro.f4m.service.game.selection.model.game.QuestionTypeUsage;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.util.random.RandomUtil;

public class QuestionSelectorTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionSelectorTest.class);

	private static final int MAX_NUMBER_OF_QUESTIONS = 300;
	private static final int NUMBER_OF_QUESTIONS = 5;

	private static final String[] QUESTION_POOLS = new String[] { "sport", "nature", "history", "music", "art" };
	private static final String[] QUESTION_TYPES = new String[] { "image", "text" };

	private static final String GAME_ID = "game_id_125";

	@Mock
	private QuestionPoolAerospikeDao questionPoolDao;

	@Mock
	private Config config;

	@Mock
	private GameHistoryManager gameHistoryManager;

	@Mock
	private RandomUtil randomUtil;

	private JsonUtil jsonUtil;
	private TestDataLoader testDataLoader;
	private QuestionSelector questionSelector;
	
	private Game game;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		
		doAnswer((args) -> RandomUtils.nextLong(0, (Long) args.getArgument(0)))
			.when(randomUtil).nextLong(anyLong());
		
		jsonUtil = new JsonUtil();
		questionSelector = new QuestionSelector(questionPoolDao, config, gameHistoryManager, randomUtil);
		testDataLoader = new TestDataLoader(null, null, config);
		
		game = GameJsonBuilder.createGame(GAME_ID)
				.withNumberOfQuestions(NUMBER_OF_QUESTIONS)
				.withGameType(GameType.QUIZ24)
				.buildGame(jsonUtil);
	}

	@Test
	public void testGetAmountOfQuestionsByType() {
		final int numberOfQuestions = 31;

		final Game game = new Game();
		game.setNumberOfQuestions(numberOfQuestions);
		
		//Test percentage values
		final int[] amountOfQuestionsInPerCent = new int[] { 85, 1, 14 };
		game.setAmountOfQuestions(amountOfQuestionsInPerCent);
		game.setQuestionTypeSpread(QuestionTypeSpread.PERCENTAGE);
		assertThat(questionSelector.getAmountOfQuestionsByType(amountOfQuestionsInPerCent, numberOfQuestions, 0, QuestionTypeSpread.PERCENTAGE),
				equalTo(new Integer[] { 26, 1, 4 }));

		//Test fixed values
		final int[] amountOfQuestionsAsFixed = new int[] { 12, 17, 2 };
		game.setAmountOfQuestions(amountOfQuestionsAsFixed);
		game.setQuestionTypeSpread(QuestionTypeSpread.FIXED);
		assertThat(questionSelector.getAmountOfQuestionsByType(amountOfQuestionsAsFixed, numberOfQuestions, 0, QuestionTypeSpread.FIXED),
				equalTo(amountOfQuestionsAsFixed));
	}

	@Test
	public void testConvertPercentageToFixed() {
		//Equal per cent with minimum 1
		assertThat(questionSelector.convertPercentageToFixed(new int[] { 80, 10, 10 }, 5),
				equalTo(new Integer[] { 3, 1, 1 }));

		//Equal per cent without minimum
		assertThat(questionSelector.convertPercentageToFixed(new int[] { 86, 7, 7 }, 293),
				equalTo(new Integer[] { 251, 21, 21 }));

		//Different per cent with minimum 1 in the middle
		assertThat(questionSelector.convertPercentageToFixed(new int[] { 14, 1, 85 }, 31),
				equalTo(new Integer[] { 4, 1, 26 }));

		//Different per cent with two minimums at the beginning
		assertThat(questionSelector.convertPercentageToFixed(new int[] { 1, 1, 98 }, 5),
				equalTo(new Integer[] { 1, 1, 3 }));

		//Test descending per cents
		assertThat(questionSelector.convertPercentageToFixed(new int[] { 45, 35, 20 }, 219),
				equalTo(new Integer[] { 98, 77, 44 }));

		//Test 0% and 100%
		assertThat(questionSelector.convertPercentageToFixed(new int[] { 0, 0, 100 }, 31),
				equalTo(new Integer[] { 0, 0, 31 }));
	}

	@Test
	public void testConvertPercentageToFixedSum100() {
		IntStream.range(0, 100)
			.forEach(i -> testConvertPercentageToFixedSum());
	}

	private void testConvertPercentageToFixedSum() {
		final Game game = new Game();

		final int numberOfQuestions = RandomUtils.nextInt(0, MAX_NUMBER_OF_QUESTIONS) + 1;

		final int p1 = RandomUtils.nextInt(0, 101);
		final int p2 = RandomUtils.nextInt(0, 101 - p1);
		final int p3 = 100 - p1 - p2;

		LOGGER.info("Testing question type distribution for number of questions[{}]: by per cents [{}% {}% {}% = {}%]",
				numberOfQuestions, p1, p2, p3, p1 + p2 + p3);

		game.setNumberOfQuestions(numberOfQuestions);
		game.setAmountOfQuestions(p1, p2, p3);
		game.setQuestionTypeSpread(QuestionTypeSpread.PERCENTAGE);

		final int[] amountOfQuestions = questionSelector.convertPercentageToFixed(new int[] { p1, p2, p3 },
				numberOfQuestions);
		final int finalNumberOfQuestions = Arrays.stream(amountOfQuestions).sum();

		assertEquals(numberOfQuestions, finalNumberOfQuestions);
	}

	@Test
	public void testGetRandomQuestionPoolHistogram() {
		final String selectedType = QUESTION_TYPES[RandomUtils.nextInt(0, QUESTION_TYPES.length)];
		final int selectedComplexity = RandomUtils.nextInt(0, MAX_COMPLEXITY);
		
		final QuestionPoolSizeIdentifier sizeIdentifier = new QuestionPoolSizeIdentifier(selectedType, selectedComplexity);
		final QuestionPoolSizeMeta sizeMeta = new QuestionPoolSizeMeta(QUESTION_POOLS);
		Arrays.stream(QUESTION_POOLS)
			.forEach(poolId -> sizeMeta.addPoolSize(poolId, RandomUtils.nextInt(0, MAX_NUMBER_OF_QUESTIONS)));

		final int[] questionSelectionHistogram = new int[QUESTION_POOLS.length];
		for (int i = 0; i < 100; i++) {
			final Pair<String, Long> poolAndSize = questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta);
			assertNotNull(poolAndSize);
			assertThat(poolAndSize.getKey(), isOneOf(QUESTION_POOLS));
			assertThat(poolAndSize.getValue(), greaterThan(0L));

			questionSelectionHistogram[ArrayUtils.indexOf(QUESTION_POOLS, poolAndSize.getKey())]++;
		}

		LOGGER.info(
				"Random Question Pool selection histogram from pools {} with {} and type[{}] resulted into {} after 100 iterations",
				QUESTION_POOLS, sizeMeta, selectedType, questionSelectionHistogram);
	}
	
	@Test
	public void testGetRandomQuestionPool() {
		final QuestionPoolSizeIdentifier sizeIdentifier = new QuestionPoolSizeIdentifier("35", 1);
		final QuestionPoolSizeMeta sizeMeta = new QuestionPoolSizeMeta(QUESTION_POOLS);
		sizeMeta.addPoolSize(QUESTION_POOLS[0], 1);
		sizeMeta.addPoolSize(QUESTION_POOLS[1], 10);
		sizeMeta.addPoolSize(QUESTION_POOLS[2], 100);
		sizeMeta.addPoolSize(QUESTION_POOLS[3], 1000);
		sizeMeta.addPoolSize(QUESTION_POOLS[4], 1);
		
		assertThat(toObject(sizeMeta.getPoolSizes()), arrayContaining(1L, 10L, 100L, 1000L, 1L));
		
		final AtomicLong nextLong = new AtomicLong();
		when(randomUtil.nextLong(anyLong())).thenAnswer((anyLong) -> nextLong.get());
		
		//Pool: 0
		nextLong.set(0);
		assertEquals(Pair.of(QUESTION_POOLS[0], 1L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		//Pool: 1
		nextLong.set(1);
		assertEquals(Pair.of(QUESTION_POOLS[1], 10L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		nextLong.set(5);
		assertEquals(Pair.of(QUESTION_POOLS[1], 10L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		nextLong.set(10);
		assertEquals(Pair.of(QUESTION_POOLS[1], 10L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		//Pool:2
		nextLong.set(11);
		assertEquals(Pair.of(QUESTION_POOLS[2], 100L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		nextLong.set(15);
		assertEquals(Pair.of(QUESTION_POOLS[2], 100L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		nextLong.set(110);
		assertEquals(Pair.of(QUESTION_POOLS[2], 100L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		//Pool: 3		
		nextLong.set(111);
		assertEquals(Pair.of(QUESTION_POOLS[3], 1000L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		nextLong.set(1000);
		assertEquals(Pair.of(QUESTION_POOLS[3], 1000L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		nextLong.set(1110);
		assertEquals(Pair.of(QUESTION_POOLS[3], 1000L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));

		//Pool: 4
		nextLong.set(1111);
		assertEquals(Pair.of(QUESTION_POOLS[4], 1L),
				questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));
		
		//No pool
		nextLong.set(1112);
		assertNotNull(questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta));
		assertNull(questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta).getKey());
		assertEquals(questionSelector.getRandomQuestionPool(sizeIdentifier, sizeMeta).getValue(), NumberUtils.LONG_ZERO);
	}
	
	@Test
	public void testGetRandomQuestionFromPoolWithRepeats() {
		assertGetRandomQuestionFromPool(QuestionOverwriteUsage.REPEATS);
	}
	
	@Test
	public void testGetRandomQuestionFromPoolNoRepeatsAndNotPlayed() {
		assertGetRandomQuestionFromPool(QuestionOverwriteUsage.NO_REPEATS);
	}
	
	private void assertGetRandomQuestionFromPool(QuestionOverwriteUsage questionOverwriteUsage){
	    final String questionIdFormat = "ID_%d";
		final int complexity = RandomUtils.nextInt(0, MAX_COMPLEXITY) + 1; //[1..max]
		final String poolId = QUESTION_POOLS[0];
		final String questionType = QUESTION_TYPES[0];
		
		final QuestionKey questionKey = new QuestionKey(poolId, questionType, complexity, ANONYMOUS_USER_LANGUAGE);
		
		reset(gameHistoryManager);
		when(config.getPropertyAsInteger(QUESTION_PULL_NO_REPEATS_RETRY_COUNT)).thenReturn(6);
		
		final List<Integer> indexes = new ArrayList<>();

		//Worst case of random question indexes: (0), (0, 1), (0, 1, 2), ...
		for (int i = 0; i < NUMBER_OF_QUESTIONS; i++) {
			for (int k = 0; k <= i; k++) {				
				indexes.add(k);				
			}
		}
		
		when(randomUtil.nextLong(anyLong()))
			.thenAnswer((args) -> (long) args.getArgument(0) - 1);
		
		//Question matching index
		final Question[] questions = new Question[NUMBER_OF_QUESTIONS];
		for(int i = 0; i < NUMBER_OF_QUESTIONS; i++){
			questions[i] = new Question();
			questions[i].setLanguage(ANONYMOUS_USER_LANGUAGE);
			questions[i].setType(questionType);
			questions[i].setComplexity(complexity);
			questions[i].setPoolId(poolId);
			questions[i].setId(String.format(questionIdFormat, i));
		}

		when(questionPoolDao.getQuestion(eq(questionKey), any(Long.class)))
			.thenAnswer((args) -> {
				final int index = Math.toIntExact((long) args.getArgument(1));
				return questions[index];
			});
		final PoolEntryProvider<Question> poolEntryProvider = (q, j) -> 
			questionPoolDao.getQuestion(questionKey, j);
		final IsPlayedRule<Question> allQustionsAlreadyPlayed = (selectedQuestion) -> 
			questionSelector.isPlayed(ANONYMOUS_USER_ID, selectedQuestion.getId());
		
		//Select questions
		final Set<SelectedPoolEntry<Question>> selectedQuestions = new HashSet<>();
		for(int i = 0; i < NUMBER_OF_QUESTIONS; i++){
			final SelectedPoolEntry<Question> question = questionSelector.getRandomQuestionFromPool(NO_REPEATS, questionKey,
					NUMBER_OF_QUESTIONS, selectedQuestions, poolEntryProvider, allQustionsAlreadyPlayed);
			assertNotNull("Failed to selection " + (i + 1) + ". question", question);
			selectedQuestions.add(question);
		}
		
		//Verify/assert
		for(int i = 0; i < NUMBER_OF_QUESTIONS; i++){
			final String questionId = String.format(questionIdFormat, i);
			assertTrue("Question with seq. index [" + i + "] is not selected", selectedQuestions.stream()
					.map(selected -> selected.getEntry().getId())
					.anyMatch(id -> id.equals(questionId)));
			verify(gameHistoryManager, times(1)).isQuestionPlayed(ANONYMOUS_USER_ID, String.format(questionIdFormat, i));
		}
	}
	
	@Test
	public void testGetRandomQuestionFromPoolRepeats(){	
		reset(gameHistoryManager);
		reset(questionPoolDao);
		final int complexity = RandomUtils.nextInt(0, MAX_COMPLEXITY);
		final QuestionKey poolKey = new QuestionKey(QUESTION_POOLS[0], QUESTION_TYPES[0], complexity, ANONYMOUS_USER_LANGUAGE);
		
		final int poolSize = RandomUtils.nextInt(5, 300);
		final long randomQuestionIndex = RandomUtils.nextLong(5, poolSize);
		
		final Question question = new Question();
		question.setId("ID_" + randomQuestionIndex);
		
		when(config.getPropertyAsInteger(QUESTION_PULL_NO_REPEATS_RETRY_COUNT)).thenReturn(3);
		
		when(randomUtil.nextLong(anyLong())).thenReturn(randomQuestionIndex);
		when(gameHistoryManager.isQuestionPlayed(eq(ANONYMOUS_USER_ID), any())).thenReturn(true);
		when(questionPoolDao.getQuestion(poolKey, randomQuestionIndex)).thenReturn(question);
	
		final PoolEntryProvider<Question> poolEntryProvider = (q, i) -> 
			questionPoolDao.getQuestion(new QuestionKey(q, ANONYMOUS_USER_LANGUAGE), i);
		final IsPlayedRule<Question> isPlayerRule = (selectedQuestion) -> 
			questionSelector.isPlayed(ANONYMOUS_USER_ID, selectedQuestion.getId());
		final SelectedPoolEntry<Question> randomQuestion = questionSelector.<Question>getRandomQuestionFromPool(REPEATS, poolKey, poolSize,
				new HashSet<>(), poolEntryProvider, isPlayerRule);

		assertNotNull(randomQuestion);

		assertEquals(question.getId(), randomQuestion.getEntry().getId());		
		verify(gameHistoryManager, times(1)).isQuestionPlayed(eq(ANONYMOUS_USER_ID), any());
		verify(questionPoolDao, times(1)).getQuestion(poolKey, randomQuestionIndex);
	}
	
	@Test
	public void testPickRandomItemFromDistributionArray(){
		final int[] distributionAllZero = new int[4];
		final Integer pickRandomItemFromDistributionArrayUsingAllZero = questionSelector.pickRandomItemFromDistributionArray(distributionAllZero);
		assertNull(pickRandomItemFromDistributionArrayUsingAllZero);
		assertThat(toObject(distributionAllZero), arrayContaining(0, 0, 0, 0));
		
		when(randomUtil.nextInt(anyInt())).thenReturn(1);
		final int[] distribution = new int[]{2, 5, 7, 12};
		final Integer pickRandomItem = questionSelector.pickRandomItemFromDistributionArray(distribution);
		assertEquals(Integer.valueOf(1), pickRandomItem);
		assertThat(toObject(distribution), arrayContaining(2, 4, 7, 12));
		
		when(randomUtil.nextInt(anyInt())).thenReturn(0);
		questionSelector.pickRandomItemFromDistributionArray(distribution);
		questionSelector.pickRandomItemFromDistributionArray(distribution);
		assertNotNull(questionSelector.pickRandomItemFromDistributionArray(distribution));
		assertThat(toObject(distribution), arrayContaining(0, 3, 7, 12));
	}
	
	@Test
	public void testGetQuestionsForQuiz24() throws IOException{			
		when(questionPoolDao.getQuestion(any(QuestionKey.class), anyLong())).thenAnswer(new Answer<Question>() {
			@Override
			public Question answer(InvocationOnMock invocation) throws Throwable {
				final QuestionKey poolKey = invocation.getArgument(0);
				final String poolId = poolKey.getPoolId();
				final String questionType = poolKey.getType();
				final String language = poolKey.getLanguage();
				final int questionComplexity = poolKey.getComplexity();
				final String questionJson = testDataLoader.getQuestions(poolId, questionType, questionComplexity)[0];
				final Question question = jsonUtil.fromJson(questionJson, Question.class);
				question.setLanguage(language);
				return question;
			}
		});
		when(questionPoolDao.getQuestionPoolSizes(eq(ASSIGNED_POOLS), any(), any(), any()))
			.thenAnswer(new Answer<Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta>>() {
				@Override
				public Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> answer(InvocationOnMock invocation) throws Throwable {
					final String[] poolIds = invocation.getArgument(0);
					
					final QuestionPoolSizeMeta sizeMeta = mock(QuestionPoolSizeMeta.class);
					when(sizeMeta.getPoolIds()).thenReturn(poolIds);
					when(sizeMeta.getPoolSizes()).thenReturn(LongStream.generate(() -> MAX_NUMBER_OF_QUESTIONS)
							.limit(poolIds.length).toArray());
					when(sizeMeta.getPoolsSizeTotal()).thenReturn((long)poolIds.length * MAX_NUMBER_OF_QUESTIONS);
					when(sizeMeta.hasAnyQuestions()).thenReturn(true);
					
					@SuppressWarnings("unchecked")
					final Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> sizes = mock(Map.class);
					when(sizes.get(any(QuestionPoolSizeIdentifier.class))).thenReturn(sizeMeta);
				
					return sizes;
				}
			});
		
		final List<Question> questions = questionSelector.getQuestions(ANONYMOUS_USER_ID, game,
				ANONYMOUS_USER_LANGUAGE, ASSIGNED_POOLS, NUMBER_OF_QUESTIONS, 0);
	
		//TYPE VS COMLEXITY	
		final int[] actualFixedAmountOfQuestionByType = new int[TestDataLoader.QUESTION_TYPES.length];
		final int[] actualFixedAmountOfQuestionByComplexity = new int[MAX_COMPLEXITY];
		for(Question question : questions){
			actualFixedAmountOfQuestionByType[ArrayUtils.indexOf(TestDataLoader.QUESTION_TYPES, question.getType())]++;
			actualFixedAmountOfQuestionByComplexity[question.getComplexity()-1]++;
		}
	
		final int[] expectedFixedAmountOfQuestionByType = questionSelector
				.getAmountOfQuestionsByType(game.getAmountOfQuestions(), NUMBER_OF_QUESTIONS, 0, game.getQuestionTypeSpread());
		assertThat(actualFixedAmountOfQuestionByType, equalTo(expectedFixedAmountOfQuestionByType));
		
		final int[] expectedFixedAmountOfQuestionByComplexity = questionSelector.getAmountOfQuestionsByComplexity(
				game.getComplexityStructure(), NUMBER_OF_QUESTIONS, 0, game.getQuestionComplexitySpread());
		assertThat(actualFixedAmountOfQuestionByComplexity, equalTo(expectedFixedAmountOfQuestionByComplexity));
	}
	
	@Test
	public void testGetQuestionsForDuel() throws IOException{
		game.setType(GameType.DUEL);

		when(questionPoolDao.getInternationalQuestionPoolSizes(eq(game.getAssignedPools()), any(), any(), any()))
			.thenAnswer(new Answer<Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta>>() {
				@Override
				public Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> answer(InvocationOnMock invocation) throws Throwable {
					final String[] poolIds = invocation.getArgument(0);
					
					final QuestionPoolSizeMeta sizeMeta = mock(QuestionPoolSizeMeta.class);
					when(sizeMeta.getPoolIds()).thenReturn(poolIds);
					when(sizeMeta.getPoolSizes()).thenReturn(LongStream.generate(() -> MAX_NUMBER_OF_QUESTIONS)
							.limit(poolIds.length).toArray());
					when(sizeMeta.getPoolsSizeTotal()).thenReturn((long)poolIds.length * MAX_NUMBER_OF_QUESTIONS);
					when(sizeMeta.hasAnyQuestions()).thenReturn(true);
					
					@SuppressWarnings("unchecked")
					final Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> sizes = mock(Map.class);
					when(sizes.get(any(QuestionPoolSizeIdentifier.class))).thenReturn(sizeMeta);
					
					return sizes;
				}
			});
		when(questionPoolDao.getQuestionByInternationlIndex(any(QuestionIndex.class), eq(ANONYMOUS_USER_LANGUAGE)))
			.thenAnswer(new Answer<Question>() {
				@Override
				public Question answer(InvocationOnMock invocation) throws Throwable {
					final QuestionIndex index = invocation.getArgument(0);
					
					final String questionJson = testDataLoader.getQuestions(index.getPoolId(), index.getType(), index.getComplexity())[0];
					final Question question = jsonUtil.fromJson(questionJson, Question.class);
					question.setLanguage(invocation.getArgument(1));
					return question;
				}
			});
		when(questionPoolDao.getInternationalQuestionIndex(any(InternationalQuestionKey.class), anyLong())).thenAnswer(new Answer<QuestionIndex>() {
			@Override
			public QuestionIndex answer(InvocationOnMock invocation) throws Throwable {
				final InternationalQuestionKey poolKey = invocation.getArgument(0);

				final QuestionIndex questionIndex = new QuestionIndex();
				questionIndex.setComplexity(poolKey.getComplexity());
				questionIndex.setPoolId(poolKey.getPoolId());
				questionIndex.setType(poolKey.getType());
				
				questionIndex.setIndex(Stream.of(poolKey.getPlayingLanguages())
						.collect(Collectors.toMap(lang -> lang, index -> (long)invocation.getArgument(1))));
				
				return questionIndex;
			}
		});
		
		final List<QuestionIndex> internationQuestionIndexes = questionSelector.getInternationQuestionIndexes(game,
				game.getAssignedPools(), game.getNumberOfQuestions(), game.getTotalSkipsAvailable());
		F4MAssert.assertSize(game.getNumberOfQuestions(), internationQuestionIndexes);
		
		final List<Question> questions = questionSelector.getQuestions(internationQuestionIndexes, ANONYMOUS_USER_LANGUAGE);
		F4MAssert.assertSize(game.getNumberOfQuestions(), internationQuestionIndexes);
		
		//TYPE VS COMLEXITY	
		final int[] actualFixedAmountOfQuestionByType = new int[TestDataLoader.QUESTION_TYPES.length];
		final int[] actualFixedAmountOfQuestionByComplexity = new int[TestDataLoader.MAX_COMPLEXITY];
		for(Question question : questions){
			actualFixedAmountOfQuestionByType[ArrayUtils.indexOf(TestDataLoader.QUESTION_TYPES, question.getType())]++;
			actualFixedAmountOfQuestionByComplexity[question.getComplexity()-1]++;
		}
	
		final int[] expectedFixedAmountOfQuestionByType = questionSelector.getAmountOfQuestionsByType(
				game.getAmountOfQuestions(), game.getNumberOfQuestions(), game.getTotalSkipsAvailable(), game.getQuestionTypeSpread());
		assertThat(actualFixedAmountOfQuestionByType, equalTo(expectedFixedAmountOfQuestionByType));
		
		final int[] expectedFixedAmountOfQuestionByComplexity = questionSelector.getAmountOfQuestionsByComplexity(
				game.getComplexityStructure(), game.getNumberOfQuestions(), game.getTotalSkipsAvailable(), game.getQuestionComplexitySpread());
		assertThat(actualFixedAmountOfQuestionByComplexity, equalTo(expectedFixedAmountOfQuestionByComplexity));
	}
	
	@Test
	public void testSelectedPoolEntryContainsInHashSet(){
		final Set<SelectedPoolEntry<QuestionBaseKey>> selctedEntry = new HashSet<>();
		IntStream.range(0, 5)
			.forEach(i-> selctedEntry.add( new SelectedPoolEntry<QuestionBaseKey>(new QuestionBaseKey("21", "35", 2), i)));
		IntStream.range(0, 5)
			.forEach(i-> selctedEntry.add( new SelectedPoolEntry<QuestionBaseKey>(new QuestionBaseKey("22", "37", 4), i)));
		
		assertFalse(selctedEntry.contains(new SelectedPoolEntry<QuestionBaseKey>(new QuestionBaseKey("21", "35", 2), 898981)));
		assertFalse(selctedEntry.contains(new SelectedPoolEntry<QuestionBaseKey>(new QuestionBaseKey("22", "37", 4), 30)));
		
		assertTrue(selctedEntry.contains(new SelectedPoolEntry<QuestionBaseKey>(new QuestionBaseKey("21", "35", 2), 0)));
		assertTrue(selctedEntry.contains(new SelectedPoolEntry<QuestionBaseKey>(new QuestionBaseKey("21", "35", 2), 1)));
		assertTrue(selctedEntry.contains(new SelectedPoolEntry<QuestionBaseKey>(new QuestionBaseKey("22", "37", 4), 2)));
		assertTrue(selctedEntry.contains(new SelectedPoolEntry<QuestionBaseKey>(new QuestionBaseKey("22", "37", 4), 3)));
	}
	
	@Test
	public void testSelectingFromPoolsWithMissingOrEmptyMetaSize() throws IOException {
		final Answer<Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta>> noQuestionsAnsqer = new Answer<Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta>>() {
			@Override
			public Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> answer(InvocationOnMock invocation)
					throws Throwable {
				final QuestionPoolSizeMeta sizeMeta = mock(QuestionPoolSizeMeta.class);
				when(sizeMeta.getPoolsSizeTotal()).thenReturn(0L);
				
				@SuppressWarnings("unchecked")
				final Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> sizes = mock(Map.class);
				when(sizes.get(any())).thenReturn(sizeMeta);
				return sizes;
			}
		};		
		when(questionPoolDao.getQuestionPoolSizes(any(), any(), any(), any()))
			.thenAnswer(noQuestionsAnsqer);
		when(questionPoolDao.getInternationalQuestionPoolSizes(any(), any(), any(), any()))
			.thenAnswer(noQuestionsAnsqer);
		
		//Regular questions
		try {
			questionSelector.getQuestions(ANONYMOUS_USER_ID, game, ANONYMOUS_USER_LANGUAGE, ASSIGNED_POOLS,
					NUMBER_OF_QUESTIONS, 0);
			fail("F4MQuestionsNotAvailableInPool expected but no exception occurred");
		} catch (F4MQuestionsNotAvailableInPool ex) {
		} catch (Exception e) {
			fail("F4MQuestionsNotAvailableInPool expected " + e.getClass().getSimpleName());
		}
		
		//International questions
		try {
			questionSelector.getInternationQuestionIndexes(game, ASSIGNED_POOLS, NUMBER_OF_QUESTIONS, 0);
			fail("F4MQuestionsNotAvailableInPool expected");
		} catch (F4MQuestionsNotAvailableInPool ex) {
		} catch (Exception e) {
			fail("F4MQuestionsNotAvailableInPool expected");
		}
	}

	@Test
	public void testQuestionsCannotBeFoundInPoolButMetaSizeIsValid() {
		when(questionPoolDao.getQuestion(any(), anyLong())).thenReturn(null);
		when(questionPoolDao.getInternationalQuestionIndex(any(), anyLong())).thenReturn(null);
		
		final Answer<Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta>> sizes = new Answer<Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta>>() {
			@Override
			public Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> answer(InvocationOnMock invocation) throws Throwable {
				final String[] poolIds = invocation.getArgument(0);
				
				final QuestionPoolSizeMeta sizeMeta = mock(QuestionPoolSizeMeta.class);
				when(sizeMeta.getPoolIds()).thenReturn(poolIds);
				when(sizeMeta.getPoolSizes()).thenReturn(LongStream.generate(() -> MAX_NUMBER_OF_QUESTIONS)
						.limit(poolIds.length).toArray());
				when(sizeMeta.getPoolsSizeTotal()).thenReturn((long)poolIds.length * MAX_NUMBER_OF_QUESTIONS);
				when(sizeMeta.hasAnyQuestions()).thenReturn(true);
				
				@SuppressWarnings("unchecked")
				final Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> sizes = mock(Map.class);
				when(sizes.get(any(QuestionPoolSizeIdentifier.class))).thenReturn(sizeMeta);
			
				return sizes;
			}
		};
		when(questionPoolDao.getQuestionPoolSizes(eq(ASSIGNED_POOLS), any(), any(), any()))
			.thenAnswer(sizes);
		when(questionPoolDao.getInternationalQuestionPoolSizes(eq(ASSIGNED_POOLS), any(), any(), any()))
			.thenAnswer(sizes);
		
		//Regular questions
		try {
			questionSelector.getQuestions(ANONYMOUS_USER_ID, game, ANONYMOUS_USER_LANGUAGE, ASSIGNED_POOLS,
					NUMBER_OF_QUESTIONS, 0);
			fail("F4MQuestionCannotBeReadFromPool expected but no exception occurred");
		} catch (F4MQuestionCannotBeReadFromPool ex) {
		} catch (Exception e) {
			fail("F4MQuestionCannotBeReadFromPool expected but was " + e.getClass().getSimpleName());
		}
		
		//International questions
		try {
			questionSelector.getInternationQuestionIndexes(game, ASSIGNED_POOLS, NUMBER_OF_QUESTIONS, 0);
			fail("F4MQuestionCannotBeReadFromPool expected");
		} catch (F4MQuestionCannotBeReadFromPool ex) {
		} catch (Exception e) {
			fail("F4MQuestionCannotBeReadFromPool expected " + e.getClass().getSimpleName());
		}
	}
	
	@Test
	public void testQuestionsOrderly() {
		when(questionPoolDao.getQuestion(any(QuestionKey.class), anyLong())).thenAnswer(new Answer<Question>() {
			@Override
			public Question answer(InvocationOnMock invocation) throws Throwable {
				final QuestionKey poolKey = invocation.getArgument(0);
				final String poolId = poolKey.getPoolId();
				final String questionType = poolKey.getType();
				final String language = poolKey.getLanguage();
				final int questionComplexity = poolKey.getComplexity();
				final String questionJson = testDataLoader.getQuestions(poolId, questionType, questionComplexity)[0];
				final Question question = jsonUtil.fromJson(questionJson, Question.class);
				question.setLanguage(language);
				return question;
			}
		});
		when(questionPoolDao.getQuestionPoolSizes(eq(ASSIGNED_POOLS), any(), any(), any()))
			.thenAnswer(new Answer<Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta>>() {
				@Override
				public Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> answer(InvocationOnMock invocation) throws Throwable {
					final String[] poolIds = invocation.getArgument(0);
					
					final QuestionPoolSizeMeta sizeMeta = mock(QuestionPoolSizeMeta.class);
					when(sizeMeta.getPoolIds()).thenReturn(poolIds);
					when(sizeMeta.getPoolSizes()).thenReturn(LongStream.generate(() -> MAX_NUMBER_OF_QUESTIONS)
							.limit(poolIds.length).toArray());
					when(sizeMeta.getPoolsSizeTotal()).thenReturn((long)poolIds.length * MAX_NUMBER_OF_QUESTIONS);
					when(sizeMeta.hasAnyQuestions()).thenReturn(true);
					
					@SuppressWarnings("unchecked")
					final Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> sizes = mock(Map.class);
					when(sizes.get(any(QuestionPoolSizeIdentifier.class))).thenReturn(sizeMeta);
				
					return sizes;
				}
			});
		
		game.setQuestionTypeUsage(QuestionTypeUsage.ORDERED);
		final List<Question> questions = questionSelector.getQuestions(ANONYMOUS_USER_ID, game,
				ANONYMOUS_USER_LANGUAGE, ASSIGNED_POOLS, NUMBER_OF_QUESTIONS, 0);
		
		assertEquals(game.getQuestionTypes()[0], questions.get(0).getType()); 
		assertEquals(game.getQuestionTypes()[0], questions.get(1).getType()); 
		assertEquals(game.getQuestionTypes()[1], questions.get(2).getType()); 
		assertEquals(game.getQuestionTypes()[1], questions.get(3).getType()); 
		assertEquals(game.getQuestionTypes()[1], questions.get(4).getType()); 

	}
	
	
	
	@Test(expected = F4MQuestionCannotBeReadFromPool.class)
	public void testQuestionIndexForParticularLanguageDoesNotExist() {
		when(questionPoolDao.getQuestionByInternationlIndex(any(), any()))
				.thenThrow(F4MQuestionCannotBeReadFromPool.class);

		questionSelector.getQuestions(Arrays.asList(new QuestionIndex()), "any");
	}
	
}
