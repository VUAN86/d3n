package de.ascendro.f4m.service.profile.model.invoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;

public class EndConsumerInvoiceListResponse extends ListResult<JsonObject> implements JsonMessageContent {

    public EndConsumerInvoiceListResponse(int limit, long offset) {
        super(limit, offset, 0, Collections.emptyList());
    }

    public EndConsumerInvoiceListResponse(int limit, long offset, long total) {
        super(limit, offset, total, new ArrayList<>());
    }

    public EndConsumerInvoiceListResponse(int limit, long offset, long total, List<JsonObject> items) {
        super(limit, offset, total, items);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EndConsumerInvoiceListResponse [");
        builder.append("items=").append(getItems());
        builder.append(", limit=").append(getLimit());
        builder.append(", offset=").append(getOffset());
        builder.append(", total=").append(getTotal());
        builder.append("]");
        return builder.toString();
    }

}
