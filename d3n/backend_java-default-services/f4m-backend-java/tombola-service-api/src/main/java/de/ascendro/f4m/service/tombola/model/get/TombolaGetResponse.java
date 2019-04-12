package de.ascendro.f4m.service.tombola.model.get;


import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.tombola.model.Tombola;

public class TombolaGetResponse implements JsonMessageContent {

    private Tombola tombola;

    public TombolaGetResponse(Tombola tombola) {
        this.tombola = tombola;
    }

    public Tombola getTombola() {
        return tombola;
    }

    public void setTombola(Tombola tombola) {
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
