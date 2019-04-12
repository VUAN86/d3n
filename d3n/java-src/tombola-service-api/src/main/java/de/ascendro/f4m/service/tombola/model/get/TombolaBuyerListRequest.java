package de.ascendro.f4m.service.tombola.model.get;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class TombolaBuyerListRequest extends FilterCriteria implements JsonMessageContent {

    public static final int MAX_LIST_LIMIT = 100;

    private String tombolaId;

    public TombolaBuyerListRequest() {
        this(MAX_LIST_LIMIT, 0);
    }

    public TombolaBuyerListRequest(int limit, long offset) {
        setLimit(limit);
        setOffset(offset);
    }

    public String getTombolaId() {
        return tombolaId;
    }

    public void setTombolaId(String tombolaId) {
        this.tombolaId = tombolaId;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TombolaBuyerListRequest [");
        builder.append("limit=").append(Integer.toString(getLimit()));
        builder.append(", tombolaId=").append(getTombolaId());
        builder.append(", offset=").append(Long.toString(getOffset()));
        builder.append(", orderBy=").append(getOrderBy());
        builder.append(", searchBy=").append(getSearchBy());
        builder.append("]");
        return builder.toString();
    }
}
