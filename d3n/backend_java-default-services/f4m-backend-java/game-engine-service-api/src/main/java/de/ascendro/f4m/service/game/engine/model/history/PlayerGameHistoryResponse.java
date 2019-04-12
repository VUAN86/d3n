package de.ascendro.f4m.service.game.engine.model.history;

import de.ascendro.f4m.service.game.engine.model.UserGameHistoryItem;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

import java.util.ArrayList;
import java.util.List;

public class PlayerGameHistoryResponse implements JsonMessageContent {
    private List<UserGameHistoryItem> items = new ArrayList<UserGameHistoryItem>();
    private long limit;
    private long offset;
    private long total;

    public PlayerGameHistoryResponse() {
        // Initialize empty response
    }

    public PlayerGameHistoryResponse(List<UserGameHistoryItem> items, long limit, long offset, long total) {
        this.items = items;
        this.limit = limit;
        this.offset = offset;
        this.total = total;
    }

    public void setItems(List<UserGameHistoryItem> items) {
        this.items = items;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setOffset(long offset) {

        this.offset = offset;
    }

    public void setLimit(long limit) {

        this.limit = limit;
    }

    public long getTotal() {
        return total;
    }

    public long getOffset() {

        return offset;
    }

    public long getLimit() {

        return limit;
    }

    public List<UserGameHistoryItem> getItems() {

        return items;
    }


}
