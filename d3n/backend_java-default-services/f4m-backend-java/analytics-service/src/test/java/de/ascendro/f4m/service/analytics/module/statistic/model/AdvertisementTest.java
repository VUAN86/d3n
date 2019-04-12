package de.ascendro.f4m.service.analytics.module.statistic.model;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdvertisementTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdvertisementTest.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testUpdateStatementCreation() throws Exception {
        String statement =  Advertisement.getUpdateStatement(Advertisement.class);
        assertTrue(!statement.isEmpty());
        LOGGER.info(statement);
    }
}
