package de.ascendro.f4m.service.result.engine.model.get;

import de.ascendro.f4m.service.json.model.FilterCriteria;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetStatisticRequest extends FilterCriteria implements JsonMessageContent {

    private int parentId;

    public GetStatisticRequest() {
        // Initialize empty request
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    @Override
    public String toString() {
        return "GetStatisticRequest{" +
                "parentId=" + parentId +
                "} " + super.toString();
    }
}
