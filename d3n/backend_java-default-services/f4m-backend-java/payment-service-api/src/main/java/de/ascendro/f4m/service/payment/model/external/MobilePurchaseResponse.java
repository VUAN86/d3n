package de.ascendro.f4m.service.payment.model.external;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class MobilePurchaseResponse implements JsonMessageContent {
    private String answer;

    public MobilePurchaseResponse(String answer) {
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
        builder.append("MobilePurchaseResponse [Answer=");
        builder.append(answer);
        builder.append("]");
        return builder.toString();
    }
}
