package de.ascendro.f4m.server.analytics.model;

import static org.testng.Assert.assertNotNull;

import org.junit.Test;


public class PlayerGameEndEventTest {

    @Test
    public void testAddQuestion() throws Exception {
        PlayerGameEndEvent playerGameEndEvent =  new PlayerGameEndEvent();

        Question question =  new Question();
        question.setQuestionId(1L);
        question.setAnswerIsCorrect(true);

        playerGameEndEvent.addQuestion(question);

        assertNotNull(playerGameEndEvent.getQuestion(0));
    }

}