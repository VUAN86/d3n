package de.ascendro.f4m.service.analytics.module.statistic.query;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.google.inject.Inject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.statistic.model.Advertisement;
import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.BaseUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.Renderer;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;

public class AdvertisementUpdater extends BaseUpdater<Advertisement> implements ITableUpdater<Advertisement> {
    @InjectLogger
    private static Logger LOGGER;

    private final Pattern pattern = Pattern.compile("provider\\_([0-9]*)\\_advertisement\\_([0-9]*)\\.json");

    @Inject
    public AdvertisementUpdater(Config config, NotificationCommon notificationUtil) {
        super(config, notificationUtil);
    }

    @Override
    public void processEvent(EventContent content) throws Exception {
        if (content.isOfType(AdEvent.class)) {
            process(content, new Renderer<Advertisement, AdEvent>() {
                @Override
                public void render(Advertisement table, AdEvent event) {
                    table.setId(event.getAdId());
                    table.setAdViews(1);
                    table.setEarnedCredits(checkLong(event.getEarnCredits()));
                    table.setAppUsed(incrementIfTrue(event.isFirstAppUsage()));
                    table.setGameUsed(incrementIfTrue(event.isFirstGameUsage()));
                }
            });
        }
    }


    @Override
    protected Class<Advertisement> getTableClass() {
        return Advertisement.class;
    }

    private void fillAdIdForEvent(AdEvent adEvent) throws SQLException {
        Matcher matcher = pattern.matcher(adEvent.getBlobKey());
        if (matcher.find()) {
            Long providerId = Long.valueOf(matcher.group(1));
            Long index = Long.valueOf(matcher.group(2));

            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(new StringBuilder("SELECT ")
                    .append(BaseStatisticTable.getKeyList(getTableClass()).get(0))
                    .append(" FROM ").append(BaseStatisticTable.getTableName(getTableClass()))
                    .append(" WHERE advertisementProviderId=? AND publishIdx=?").toString());

            preparedStatement.setLong(1, providerId);
            preparedStatement.setLong(2, index);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                adEvent.setAdId(resultSet.getLong(1));
            } else {
                LOGGER.error("Could not find Advertisement for ProviderId[{}] and publishIdx[{}]", providerId, index);
            }

            preparedStatement.close();
            connection.close();
        }
    }

    @Override
    protected <T> void preProcess(EventContent content, T event) throws SQLException {
        if (event instanceof AdEvent) {
            AdEvent adEvent = (AdEvent)event;
            if (StringUtils.isNotBlank(adEvent.getBlobKey())) {
                fillAdIdForEvent(adEvent);
            }
        }
    }
}
