package de.ascendro.f4m.service.tombola.model.get;


import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class UserTombolaListRequest extends FilterCriteria implements JsonMessageContent {
    private String dateFrom;

    private String dateTo;

    private Boolean pendingDrawing;

    /** Maximum allowed requested list limit. */
    public static final int MAX_LIST_LIMIT = 100;

    public UserTombolaListRequest() {
        this(MAX_LIST_LIMIT, 0, null, null, null);
    }

    public UserTombolaListRequest(long offset) {
        this(MAX_LIST_LIMIT, offset, null, null, null);
    }

    public UserTombolaListRequest(int limit, long offset) {
        this(limit, offset, null, null, null);
    }

    public UserTombolaListRequest(int limit, long offset, String dateFrom, String dateTo, Boolean pendingDrawing) {
        setLimit(limit);
        setOffset(offset);
        setDateFrom(dateFrom);
        setDateTo(dateTo);
        setPendingDrawing(pendingDrawing);
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

    public Boolean getPendingDrawing() {
        return pendingDrawing;
    }

    public void setPendingDrawing(Boolean pendingDrawing) {
        this.pendingDrawing = pendingDrawing;
    }
}
