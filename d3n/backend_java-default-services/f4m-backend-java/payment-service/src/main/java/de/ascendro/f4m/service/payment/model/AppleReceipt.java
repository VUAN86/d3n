package de.ascendro.f4m.service.payment.model;

import java.util.List;

public class AppleReceipt extends com.google.api.client.json.GenericJson {

    @com.google.api.client.util.Key
    private List<IosInApp> in_app;

    public List<IosInApp> getIn_app() {
        return in_app;
    }

    public void setIn_app(List<IosInApp> in_app) {
        this.in_app = in_app;
    }
}
