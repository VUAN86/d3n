package de.ascendro.f4m.service.game.engine.server;

import static de.ascendro.f4m.service.game.engine.config.GameEngineConfig.QUESTION_PULL_FRAGMENTATION_RETRY_COUNT;
import static de.ascendro.f4m.service.game.engine.config.GameEngineConfig.QUESTION_PULL_FRAGMENTATION_RETRY_COUNT_DEFAULT;
import static de.ascendro.f4m.service.game.engine.config.GameEngineConfig.QUESTION_PULL_NO_REPEATS_RETRY_COUNT;
import static de.ascendro.f4m.service.game.engine.config.GameEngineConfig.QUESTION_PULL_NO_REPEATS_RETRY_COUNT_DEFAULT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolAerospikeDao;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolSizeIdentifier;
import de.ascendro.f4m.service.game.engine.dao.pool.QuestionPoolSizeMeta;
import de.ascendro.f4m.service.game.engine.exception.F4MQuestionCannotBeReadFromPool;
import de.ascendro.f4m.service.game.engine.exception.F4MQuestionsNotAvailableInPool;
import de.ascendro.f4m.service.game.engine.history.GameHistoryManager;
import de.ascendro.f4m.service.game.engine.model.InternationalQuestionKey;
import de.ascendro.f4m.service.game.engine.model.Question;
import de.ascendro.f4m.service.game.engine.model.QuestionBaseKey;
import de.ascendro.f4m.service.game.engine.model.QuestionIndex;
import de.ascendro.f4m.service.game.engine.model.QuestionKey;
import de.ascendro.f4m.service.game.selection.model.game.QuestionComplexitySpread;
import de.ascendro.f4m.service.game.selection.model.game.QuestionOverwriteUsage;
import de.ascendro.f4m.service.game.selection.model.game.QuestionPoolSelectionProperties;
import de.ascendro.f4m.service.game.selection.model.game.QuestionTypeSpread;
import de.ascendro.f4m.service.game.selection.model.game.QuestionTypeUsage;
import de.ascendro.f4m.service.util.random.OutOfUniqueRandomNumbersException;
import de.ascendro.f4m.service.util.random.RandomSequenceGenerator;
import de.ascendro.f4m.service.util.random.RandomUtil;

public class QuestionSelector {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(QuestionSelector.class);
	
	private final QuestionPoolAerospikeDao questionPoolDao;
	private final Config config;

	private final GameHistoryManager gameHistoryManager;
	private final RandomUtil randomUtil;

	@Inject
	public QuestionSelector(QuestionPoolAerospikeDao questionPoolDao, Config config,
			GameHistoryManager gameHistoryManager, RandomUtil randomUtil) {
		this.questionPoolDao = questionPoolDao;
		this.config = config;
		this.gameHistoryManager = gameHistoryManager;
		this.randomUtil = randomUtil;
	}
	
	public List<Question> getQuestions(List<QuestionIndex> questionIndexes, String userLanguage){
		final List<Question> questions = new ArrayList<>(questionIndexes.size());
		
		for(QuestionIndex questionIndex : questionIndexes){
			questions.add(questionPoolDao.getQuestionByInternationlIndex(questionIndex, userLanguage));
		}
		
		return questions;
	}

	/**
	 * Select questions for particular user
	 * @param userId - user id
	 * @param selectionConfig - question selection configuration from game
	 * @param userLanguage - user preferred language for questions
	 * @param poolIds - pools to use
	 * @param numberOfQuestions - number of questions to select
	 * @return list of selected questions
	 * @throws F4MEntryNotFoundException - if failed to obtain questions
	 */
	public List<Question> getQuestions(String userId, QuestionPoolSelectionProperties selectionConfig, String userLanguage, String[] poolIds,
			int numberOfQuestions, int skipsAvailable) throws F4MEntryNotFoundException {
		final PoolSizesProvider poolSizesProvider = (questionTypesToSelect, questionComplexitiesToSelect) -> 
			questionPoolDao.getQuestionPoolSizes(poolIds, questionTypesToSelect, questionComplexitiesToSelect, userLanguage);
		
		final PoolEntryProvider<Question> poolEntryProvider = (baseKey, index) -> 
			questionPoolDao.getQuestion(new QuestionKey(baseKey, userLanguage),  index);
			
		return selectQuestions(selectionConfig, numberOfQuestions, skipsAvailable, poolSizesProvider,
				poolEntryProvider, question -> isPlayed(userId, question.getId()));
	}
	
	/**
	 * Select international question indexes using same algorithm used for question selection, 
	 * so for selected indexes same rules apply as for regular questions (except checking if player has played question before)
	 * @param selectionConfig - question selection configuration from game
	 * @param poolIds - pools to use 
	 * @param numberOfQuestions
	 * @return list of selected international question indexes
	 * @throws F4MEntryNotFoundException - if failed to obtain questions
	 */
	public List<QuestionIndex> getInternationQuestionIndexes(QuestionPoolSelectionProperties selectionConfig, String[] poolIds,
			int numberOfQuestions, int skipsAvailable) {
		final String[] playingLangauges = selectionConfig.getPlayingLanguages();

		final PoolSizesProvider poolSizesProvider = (questionTypesToSelect, questionComplexitiesToSelect) -> 
			questionPoolDao.getInternationalQuestionPoolSizes(poolIds, questionTypesToSelect, questionComplexitiesToSelect, playingLangauges);
		final PoolEntryProvider<QuestionIndex> poolEntryProvider = (baseKey, index) -> 
			questionPoolDao.getInternationalQuestionIndex(new InternationalQuestionKey(baseKey, playingLangauges),  index);

		return selectQuestions(selectionConfig, numberOfQuestions, skipsAvailable, poolSizesProvider, poolEntryProvider, 
				anyQuestionIndex -> false);
	}

	private <Q extends QuestionBaseKey> List<Q> selectQuestions(QuestionPoolSelectionProperties selectionConfig, int numberOfQuestions, int skipsAvailable,
			PoolSizesProvider poolSizesProvider, PoolEntryProvider<Q> poolEntryProvider, IsPlayedRule<Q> isPlayedRule) {
		//TODO: QuestionTypeUsage and QuestionSelection not used yet, but have values within Game Studio
		//		final QuestionSelection questionSelection = QuestionSelection.RANDOM_PER_PLAYER;
		final QuestionTypeUsage questionTypeUsage = selectionConfig.getQuestionTypeUsage();
		final String[] questionTypes = selectionConfig.getQuestionTypes();
		final QuestionOverwriteUsage questionOverwriteUsage = selectionConfig.getQuestionOverwriteUsage();
		
		//Amount of questions
		final int[] fixedAmountOfQuestionsByType = getAmountOfQuestionsByType(selectionConfig.getAmountOfQuestions(),
				numberOfQuestions, skipsAvailable, selectionConfig.getQuestionTypeSpread());
		final int[] fixedAmountOfQuestionsByComplexity = getAmountOfQuestionsByComplexity(selectionConfig.getComplexityStructure(),
				numberOfQuestions, skipsAvailable, selectionConfig.getQuestionComplexitySpread());

		//Distribution of questions		
		final String[] questionTypesToSelect = IntStream.range(0, fixedAmountOfQuestionsByType.length)
				.filter(i -> fixedAmountOfQuestionsByType[i] > 0)
				.mapToObj(i -> questionTypes[i])
				.toArray(String[]::new);
		final int[] questionComplexitiesToSelect = IntStream.range(0, fixedAmountOfQuestionsByComplexity.length)
				.filter(i -> fixedAmountOfQuestionsByComplexity[i] > 0)
				.map(i -> i + 1) //index to complexity: 0..n-1 -> 1..n
				.toArray();

		final Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> questionPoolsSizes = poolSizesProvider
				.getPoolSizes(questionTypesToSelect, questionComplexitiesToSelect);
		
		final List<Q> questions = new ArrayList<>(numberOfQuestions);
		final Set<SelectedPoolEntry<Q>> selectedEntryIndex = new HashSet<>();
		for (int q = 0; q < numberOfQuestions; q++) {
			final Integer questionTypeIndex = pickItemFromDistributionArray(fixedAmountOfQuestionsByType, questionTypeUsage);			
			final Integer complexityIndex = pickRandomItemFromDistributionArray(fixedAmountOfQuestionsByComplexity);

			//Validate exit criteria
			validateQuestionSelectionState(fixedAmountOfQuestionsByType, fixedAmountOfQuestionsByComplexity, questionTypeIndex, complexityIndex);

			final String type = questionTypes[questionTypeIndex];
			final int complexity = complexityIndex + 1; //0..n-1 -> 1..n

			final QuestionPoolSizeIdentifier sizeIdentifier = new QuestionPoolSizeIdentifier(type, complexity);
			final QuestionPoolSizeMeta sizeMeta = questionPoolsSizes.get(sizeIdentifier);
			if (sizeMeta.hasAnyQuestions()) {
				final Pair<String, Long> poolIdAndSize = getRandomQuestionPool(sizeIdentifier, sizeMeta);
				if (poolIdAndSize.getValue() != null && poolIdAndSize.getValue() > 0) {
					final QuestionBaseKey questionBaseKey = new QuestionBaseKey(poolIdAndSize.getKey(), type,
							complexity);
					final SelectedPoolEntry<Q> poolEntry = getRandomQuestionFromPool(questionOverwriteUsage,
							questionBaseKey, poolIdAndSize.getValue(), selectedEntryIndex, poolEntryProvider,
							isPlayedRule);
					questions.add(poolEntry.getEntry());
					selectedEntryIndex.add(poolEntry);
				} else {
					throw new F4MQuestionsNotAvailableInPool(
							String.format("Selected question pool %s with complexity [%d] and type [%s] is of total size 0",
									poolIdAndSize.getKey(), sizeIdentifier.getCompelxity(),
									sizeIdentifier.getType()));
				}
			} else {
				throw new F4MQuestionsNotAvailableInPool(
						String.format("Total size of all referenced pools %s with complexity [%d] and type [%s] is 0",
								Arrays.toString(sizeMeta.getPoolIds()), sizeIdentifier.getCompelxity(),
								sizeIdentifier.getType()));
			}
		}

		return questions;
	}

	/**
	 * Validate question selection state: -- if questions are picked according to complexity and type distributions and
	 * total count match game setting
	 * 
	 * @param fixedAmountOfQuestionsByType
	 *            - absolute distribution of left question by its type
	 * @param fixedAmountOfQuestionsByComplexity
	 *            - absolute distribution of left question by its complexity
	 * @param questionTypeIndex
	 *            -- picked question type index
	 * @param questionComplexityIndex
	 *            -- picked question complexity index
	 */
	private void validateQuestionSelectionState(final int[] fixedAmountOfQuestionsByType,
			final int[] fixedAmountOfQuestionsByComplexity, final Integer questionTypeIndex,
			final Integer questionComplexityIndex) {
		if ((questionTypeIndex != null && questionComplexityIndex == null)
				|| (questionTypeIndex == null && questionComplexityIndex != null)) {
			throw new F4MFatalErrorException(String.format(
					"Question distribtion arrays were drained unequaly: distribution of types %s and type [%d], distribution of complexities %s and complexity %d",
					Arrays.toString(fixedAmountOfQuestionsByType), questionTypeIndex,
					Arrays.toString(fixedAmountOfQuestionsByComplexity), questionComplexityIndex));
		} else if (questionTypeIndex == null) {
			throw new F4MFatalErrorException(
					"Question distribtion arrays were drained before total number of questions was collected: types["
							+ fixedAmountOfQuestionsByType + "] and type[" + questionTypeIndex + "], complexities["
							+ fixedAmountOfQuestionsByComplexity + "] and complexity[" + questionComplexityIndex + "]");
		}
	}

	/**
	 * Pick random item with occurrence > 0 and decrement it occurrences
	 * 
	 * @param distribution
	 *            -- array which described item distribution: int[]{3, 5, 7} -> item 0 - 3 occurrences, item 1 - 5
	 *            occurrences and item 2 - 7 occurrences
	 * @return item index within the array or null if distribution array is drained (all values are zero)
	 */
	protected Integer pickRandomItemFromDistributionArray(int[] distribution) {
		return pickItemFromDistributionArray(distribution,QuestionTypeUsage.RANDOM);
	}

	private Integer pickItemFromDistributionArray(int[] distribution, QuestionTypeUsage questionTypeUsage) {
		final int[] availableOccuranceIndexes = IntStream.range(0, distribution.length)
				.filter(i -> distribution[i] > 0)
				.toArray();

		Integer itemIndex;
		if (!ArrayUtils.isEmpty(availableOccuranceIndexes)) {
			if (QuestionTypeUsage.RANDOM.equals(questionTypeUsage)){
				itemIndex = availableOccuranceIndexes[randomUtil.nextInt(availableOccuranceIndexes.length)];
			} else {
				itemIndex = availableOccuranceIndexes[0];
			}
			distribution[itemIndex]--;
		} else {
			itemIndex = null;
		}

		return itemIndex;
	}
	

	protected int[] getAmountOfQuestionsByType(int[] configuredAmountOfQuestions, int numberOfQuestions, int skipsAvailable, QuestionTypeSpread questionTypeSpread) {
		Validate.notNull(questionTypeSpread, "Game question type spread must not be null");

		final int[] fixedAmountOfQuestions;
		switch (questionTypeSpread) {
			case PERCENTAGE:
				fixedAmountOfQuestions = convertPercentageToFixed(configuredAmountOfQuestions, numberOfQuestions);
				break;
			case FIXED:
			default:
				fixedAmountOfQuestions = adjustToSkipsAvailable(configuredAmountOfQuestions, skipsAvailable);
		}
		return fixedAmountOfQuestions;
	}

	protected int[] getAmountOfQuestionsByComplexity(int[] complexityStructure, int numberOfQuestions, int skipsAvailable, QuestionComplexitySpread questionComplexitySpread) {
		Validate.notNull(questionComplexitySpread, "Game question complexity spread must not be null");

		final int[] fixedAmountOfQuestions;
		switch (questionComplexitySpread) {
			case PERCENTAGE:
				fixedAmountOfQuestions = convertPercentageToFixed(complexityStructure, numberOfQuestions);
				break;
			case FIXED:
			default:
				fixedAmountOfQuestions = adjustToSkipsAvailable(complexityStructure, skipsAvailable);
		}

		return fixedAmountOfQuestions;
	}

	protected int[] convertPercentageToFixed(int[] amountOfQuestionsInPercent, int numberOfQuestions) {
		int[] copiedAmountOfQuestionsInPercent = Arrays.copyOf(amountOfQuestionsInPercent, amountOfQuestionsInPercent.length);
		final int[] orderedAmountOfQuestionsInPercent = amountOfQuestionsInPercent.clone();
		Arrays.sort(orderedAmountOfQuestionsInPercent);

		final int[] amountOfQuestions = new int[copiedAmountOfQuestionsInPercent.length];

		int total = 0;
		for (int i = 0; i < orderedAmountOfQuestionsInPercent.length - 1; i++) {//all questions except last one
			final int p = orderedAmountOfQuestionsInPercent[i];
			final int pIndex = ArrayUtils.indexOf(copiedAmountOfQuestionsInPercent, p);
			final int roundedCount = Math.round((p * numberOfQuestions) / 100f);
			amountOfQuestions[pIndex] = roundedCount == 0 && p > 0 ? 1 : roundedCount;
			copiedAmountOfQuestionsInPercent[pIndex] = -1;//mark as used
			total += amountOfQuestions[pIndex];
		}
		final int pIndex = ArrayUtils.indexOf(copiedAmountOfQuestionsInPercent,
				orderedAmountOfQuestionsInPercent[orderedAmountOfQuestionsInPercent.length - 1]);
		amountOfQuestions[pIndex] = numberOfQuestions - total;

		return amountOfQuestions;
	}
	
	private int[] adjustToSkipsAvailable(int[] fixedNumberOfQuestions, int skipsAvailable) {
		if (skipsAvailable <= 0) {
			return fixedNumberOfQuestions;
		}
		int[] amountOfQuestions = Arrays.copyOf(fixedNumberOfQuestions, fixedNumberOfQuestions.length);
		if (amountOfQuestions.length > 0) {
			// Increase number of questions for last type by skips available
			amountOfQuestions[amountOfQuestions.length - 1] = amountOfQuestions[amountOfQuestions.length - 1] + skipsAvailable;
		}
		return amountOfQuestions;
	}

	/**
	 * Decide which pool to use based on their size.
	 * 
	 * Example:
	 * 	There are pools: a, b and c.
	 * 	Pools has 3, 7 and 12 questions respectively.
	 *  All pools in total have 22 questions.
	 *  Random number will be selected from range 22.
	 *  Based on range [0-2; 3-9; 10-21] that random number belongs to pool will be selected
	 * @param questionPoolsSizes
	 * @param questionPoolIds
	 * @param type
	 * @param complexity
	 * @return random pool id and its size
	 */
	protected Pair<String, Long> getRandomQuestionPool(QuestionPoolSizeIdentifier sizeIdentifier, QuestionPoolSizeMeta sizeMeta) {
		LOGGER.debug("Attempting to select random pool by size Identifier[{}] from sizeMeta[{}]", 
				sizeIdentifier, sizeMeta);
		
		String poolId = null;
		long poolSize = 0L;

		final long totalQuestionCount = sizeMeta.getPoolsSizeTotal();
		if (sizeMeta.hasAnyQuestions()) {
			final long questionIndex = randomUtil.nextLong(totalQuestionCount);
			
			//convert index to pool id
			int poolIndex = 0;
			final long[] poolSizes = sizeMeta.getPoolSizes();
			for (int i = 0; i < poolSizes.length; i++){
				poolSize = poolSizes[i];
			
				if (questionIndex < poolIndex + poolSize) {
					poolId = sizeMeta.getPoolIds()[i];
					break;
				}
				poolIndex += poolSize;
			}
		} else {
			throw new F4MQuestionsNotAvailableInPool(String.format(
					"Referenced pools %s with complexity[%d] and type [%s] are of total size 0",
					Arrays.toString(sizeMeta.getPoolIds()), sizeIdentifier.getCompelxity(), sizeIdentifier.getType()));
		}
		
		LOGGER.debug("Selected random pool [{}] by estimated sizes[{}]", poolId, sizeMeta);
		return Pair.of(poolId, poolId != null ? poolSize : 0L);
	}

	@FunctionalInterface
	interface IsPlayedRule<Q extends QuestionBaseKey>{
		boolean isPlayed(Q poolEntry);
	}
	
	/**
	 * Pick random question from specified pool (poolId + questionType + language + complexity). According to specified
	 * question selection strategy (@see overwriteUsage) pick not played question (gameId + userId) or any question.
	 * 
	 * @param overwriteUsage
	 *            - question selection strategy (not played questions by player, or any questions)
	 * @param questionBaseKey
	 *            - Question pool entry base key
	 * @param userId
	 *            - Player user id
	 * @param poolSize
	 *            - Size of the question pool
	 * @param questionIndexesToExclude
	 *            - Pool entry base keys to exclude from result (attempt to exclude if possible)
	 * @param poolEntryProvider - pool entry provider by full pool entry key based on key base
	 * @return Random question from the pool specified
	 */
	protected <Q extends QuestionBaseKey> SelectedPoolEntry<Q> getRandomQuestionFromPool(QuestionOverwriteUsage overwriteUsage,
			QuestionBaseKey questionBaseKey, long poolSize, Set<SelectedPoolEntry<Q>> questionsToExclude,
			PoolEntryProvider<Q> poolEntryProvider, IsPlayedRule<Q> isPlayerRule) {
		SelectedPoolEntry<Q> selectedEntry = null;
		final RandomSequenceGenerator randomSequenceGenerator = new RandomSequenceGenerator(poolSize, randomUtil);
		try {
			final int maxRepeatsRetryCount = getMaxRetryCountForNoRepeats();
			int retryCount = 0;
			boolean isPlayed;
			do {//ensure question does not appear in previous games
				selectedEntry = readRandomPoolEntry(randomSequenceGenerator, questionBaseKey, poolEntryProvider);
				isPlayed = questionsToExclude.contains(selectedEntry) 
						|| isPlayerRule.isPlayed(selectedEntry.getEntry());
			} while (overwriteUsage == QuestionOverwriteUsage.NO_REPEATS && isPlayed && retryCount < maxRepeatsRetryCount);
		} catch (OutOfUniqueRandomNumbersException e) {
			LOGGER.warn("Could not select random question from pool {} with size {} as out of available indexes",
					questionBaseKey, poolSize, e);
		}		
		return selectedEntry;
	}
	
	private <Q extends QuestionBaseKey> SelectedPoolEntry<Q> readRandomPoolEntry(RandomSequenceGenerator randomSequenceGenerator,
			QuestionBaseKey questionBaseKey, PoolEntryProvider<Q> poolEntryProvider)
			throws OutOfUniqueRandomNumbersException {
		final int maxFragmentationRetryCount = getMaxRetryCountForFragmentation();
		int retryCount = 0;

		Q poolEntry;
		long index;
		do {// select question
			index = randomSequenceGenerator.nextLong();
			poolEntry = poolEntryProvider.getEntry(questionBaseKey, index);
			if (poolEntry == null) {
				LOGGER.warn("Failed to obtain question/index by key[{}] at index[{}] ", questionBaseKey, index);
			}
			retryCount++;
		} while (poolEntry == null && retryCount < maxFragmentationRetryCount);
		
		if (poolEntry != null) {
			return new SelectedPoolEntry<>(poolEntry, index);
		} else {
			throw new F4MQuestionCannotBeReadFromPool(
					String.format("Failed to obtain question by key[%s] and random index after %d attempts",
							questionBaseKey.toString(), maxFragmentationRetryCount));
		}
	}

	private int getMaxRetryCountForNoRepeats() {
		final Integer maxRetryCount = config.getPropertyAsInteger(QUESTION_PULL_NO_REPEATS_RETRY_COUNT);
		return maxRetryCount != null ? maxRetryCount : QUESTION_PULL_NO_REPEATS_RETRY_COUNT_DEFAULT;
	}
	
	private int getMaxRetryCountForFragmentation() {
		final Integer maxRetryCount = config.getPropertyAsInteger(QUESTION_PULL_FRAGMENTATION_RETRY_COUNT);
		return maxRetryCount != null ? maxRetryCount : QUESTION_PULL_FRAGMENTATION_RETRY_COUNT_DEFAULT;
	}

	protected boolean isPlayed(String userId, String questionId) {
		return gameHistoryManager.isQuestionPlayed(userId, questionId);
	}
	
	static class SelectedPoolEntry<Q extends QuestionBaseKey> {
		private final Q entry;
		private final long index;

		public SelectedPoolEntry(Q entry, long index) {
			this.entry = entry;
			this.index = index;
		}
		
		public Q getEntry() {
			return entry;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((entry == null) ? 0 : entry.hashCode());
			result = prime * result + (int) (index ^ (index >>> 32));
			return result;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof SelectedPoolEntry))
				return false;
			SelectedPoolEntry<?> other = (SelectedPoolEntry<?>) obj;
			if (entry == null) {
				if (other.entry != null)
					return false;
			} else if (!entry.equals(other.entry))
				return false;
			if (index != other.index)
				return false;
			return true;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("SelectedPoolEntry [entry=");
			builder.append(entry);
			builder.append(", index=");
			builder.append(index);
			builder.append("]");
			return builder.toString();
		}		
	}
	
	@FunctionalInterface
	static interface PoolSizesProvider {
		Map<QuestionPoolSizeIdentifier, QuestionPoolSizeMeta> getPoolSizes(String[] types, int[] complexities);
	}

	@FunctionalInterface
	static interface PoolEntryProvider<Q extends QuestionBaseKey> {
		Q getEntry(QuestionBaseKey questionBaseKey, long index);
	}

}
