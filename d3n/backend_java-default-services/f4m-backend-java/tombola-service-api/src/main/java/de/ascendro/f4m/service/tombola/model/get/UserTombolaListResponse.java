package de.ascendro.f4m.service.tombola.model.get;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.ListResult;
import de.ascendro.f4m.service.tombola.model.UserTombolaInfo;

public class UserTombolaListResponse extends ListResult<UserTombolaInfoResponseModel> implements JsonMessageContent {

    public UserTombolaListResponse(int limit, long offset) {
        super(limit, offset, 0, Collections.emptyList());
    }

    public UserTombolaListResponse(int limit, long offset, long total) {
        super(limit, offset, total, new ArrayList<>());
    }

    public UserTombolaListResponse(int limit, long offset, long total, List<UserTombolaInfo> items) {
        super(limit, offset, total);
        List<UserTombolaInfoResponseModel> responseItems = new ArrayList<>();
        if (items != null) {
            items.forEach(userTombolaInfo -> responseItems.add(new UserTombolaInfoResponseModel(userTombolaInfo)));
        }
        this.setItems(responseItems);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserTombolaListResponse [");
        builder.append("items=").append(getItems());
        builder.append("]");
        return builder.toString();
    }
}
