package de.ascendro.f4m.service.game.engine.dao.preselected;

import java.util.List;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;

import de.ascendro.f4m.service.game.engine.model.QuestionIndex;

public interface PreselectedQuestionsDao {
	List<QuestionIndex> getQuestionIndexesByMgi(String mgiId);

	/**
	 * Attempt to create MGI pre-selected question set record, if not exist. 
	 * @param mgiId - multiplayer game instance identifier
	 * @param creatorId - initiator/creator id as user id
	 * @param questionIndexes - array of selected question indexes
	 * @throws AerospikeException - {@link ResultCode} KEY_EXISTS_ERROR in case of already existing pre-selected questions set
	 */
	void create(String mgiId, String creatorId, List<QuestionIndex> questionIndexes);
	
	String getCreatorId(String mgiId);
}
