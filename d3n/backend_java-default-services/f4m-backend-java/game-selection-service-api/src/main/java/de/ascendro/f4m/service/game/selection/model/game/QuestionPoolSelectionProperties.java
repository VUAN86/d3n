package de.ascendro.f4m.service.game.selection.model.game;

public interface QuestionPoolSelectionProperties {

	QuestionOverwriteUsage getQuestionOverwriteUsage();

	QuestionTypeSpread getQuestionTypeSpread();

	String[] getQuestionTypes();

	int[] getAmountOfQuestions();

	QuestionComplexitySpread getQuestionComplexitySpread();

	int[] getComplexityStructure();

	String[] getPlayingLanguages();

	boolean hasInternationalQuestions();

	QuestionTypeUsage getQuestionTypeUsage();
	
	void setQuestionTypeUsage(QuestionTypeUsage questionTypeUsage);
	
	
}