package de.ascendro.f4m.service.tombola.model.drawing;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonRequiredNullable;

public class TombolaDrawingListRequest extends FilterCriteria implements JsonMessageContent {

    @JsonRequiredNullable
    private String dateFrom;

    @JsonRequiredNullable
    private String dateTo;

    /** Maximum allowed requested list limit. */
    public static final int MAX_LIST_LIMIT = 100;

    public TombolaDrawingListRequest() {
        this(MAX_LIST_LIMIT, 0, null, null);
    }

    public TombolaDrawingListRequest(long offset) {
        this(MAX_LIST_LIMIT, offset, null, null);
    }

    public TombolaDrawingListRequest(int limit, long offset) {
        this(limit, offset, null, null);
    }

    public TombolaDrawingListRequest(int limit, long offset, String dateFrom, String dateTo) {
        setLimit(limit);
        setOffset(offset);
        setDateFrom(dateFrom);
        setDateTo(dateTo);
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }
}
