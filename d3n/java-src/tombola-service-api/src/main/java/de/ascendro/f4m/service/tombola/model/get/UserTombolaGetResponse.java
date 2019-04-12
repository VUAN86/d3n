package de.ascendro.f4m.service.tombola.model.get;


import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserTombolaGetResponse implements JsonMessageContent {

    private UserTombolaInfoResponseModel tombola;

    public UserTombolaGetResponse(UserTombolaInfoResponseModel tombola) {
        this.tombola = tombola;
    }

    public UserTombolaInfoResponseModel getTombola() {
        return tombola;
    }

    public void setTombola(UserTombolaInfoResponseModel tombola) {
        this.tombola = tombola;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TombolaGetResponse [");
        builder.append("tombola=").append(tombola.toString());
        builder.append("]");
        return builder.toString();
    }
}
