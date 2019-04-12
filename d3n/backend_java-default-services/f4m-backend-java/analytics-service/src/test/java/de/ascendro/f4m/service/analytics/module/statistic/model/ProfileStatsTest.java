package de.ascendro.f4m.service.analytics.module.statistic.model;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProfileStatsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileTest.class);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testUpdateStatementCreation() throws Exception {
        String statement =  ProfileStats.getUpdateStatement(ProfileStats.class);
        assertTrue(!statement.isEmpty());
        LOGGER.info(statement);
    }
}
