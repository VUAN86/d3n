package de.ascendro.f4m.service.winning.model;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserWinningResponse implements JsonMessageContent {
    private String answer;

    public UserWinningResponse(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserWinningResponse [Answer=");
        builder.append(answer);
        builder.append("]");
        return builder.toString();
    }
}
