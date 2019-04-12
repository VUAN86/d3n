package de.ascendro.f4m.service.tombola.model.buy;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class TombolaBuyRequest implements JsonMessageContent {

    private String tombolaId;
    private Integer tickets;

    public String getTombolaId() {
        return tombolaId;
    }

    public void setTombolaId(String tombolaId) {
        this.tombolaId = tombolaId;
    }

    public Integer getTickets() {
        return tickets;
    }

    public void setTickets(Integer tickets) {
        this.tickets = tickets;
    }
}
