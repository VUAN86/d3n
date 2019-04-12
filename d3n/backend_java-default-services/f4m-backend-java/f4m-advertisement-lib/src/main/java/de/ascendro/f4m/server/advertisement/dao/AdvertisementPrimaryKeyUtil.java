package de.ascendro.f4m.server.advertisement.dao;

import com.google.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class AdvertisementPrimaryKeyUtil extends PrimaryKeyUtil<Long> {
    private static final String ADVERTISEMENT_KEY_FORMAT = "provider:%d:counter";

    @Inject
    public AdvertisementPrimaryKeyUtil(Config config) {
        super(config);
    }

    @Override
    public String createPrimaryKey(Long providerId) {
        return String.format(ADVERTISEMENT_KEY_FORMAT, providerId);
    }
}