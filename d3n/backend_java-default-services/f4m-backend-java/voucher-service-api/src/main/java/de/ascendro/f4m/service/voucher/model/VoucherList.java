package de.ascendro.f4m.service.voucher.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

public class VoucherList extends JsonObjectWrapper {

    public static final String VOUCHER_LIST_ITEMS_PROPERTY = "items";

    public VoucherList() {
        super();
    }

    public VoucherList(JsonObject jsonObject) throws IllegalArgumentException {
        super(jsonObject);
    }


    public JsonArray getItemsAsJsonArray() {
        return this.jsonObject.getAsJsonArray(VOUCHER_LIST_ITEMS_PROPERTY);
    }
}
