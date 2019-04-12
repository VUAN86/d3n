package de.ascendro.f4m.server.market.util;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

import javax.inject.Inject;

public class MarketPrimaryKeyUtil extends PrimaryKeyUtil<String> {

    @Inject
    public MarketPrimaryKeyUtil(Config config) {
        super(config);
    }

    public String createMetaKey(String className, String id) {
        return className + KEY_ITEM_SEPARATOR + id;
    }
}
