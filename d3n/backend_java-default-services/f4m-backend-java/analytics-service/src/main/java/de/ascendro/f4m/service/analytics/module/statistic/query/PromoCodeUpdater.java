package de.ascendro.f4m.service.analytics.module.statistic.query;

import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.PromoCodeEvent;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.statistic.model.PromoCode;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.BaseUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.Renderer;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;

public class PromoCodeUpdater extends BaseUpdater<PromoCode> implements ITableUpdater<PromoCode> {

    @InjectLogger
    private static Logger LOGGER;

    @Inject
    public PromoCodeUpdater(Config config, NotificationCommon notificationUtil) {
        super(config, notificationUtil);
    }

    @Override
    public void processEvent(EventContent content) throws Exception {
        if (content.isOfType(PromoCodeEvent.class)) {
            process(content, new Renderer<PromoCode, PromoCodeEvent>() {
                @Override
                public void render(PromoCode table, PromoCodeEvent event) {
                    table.setId(event.getPromoCodeCampaignId());
                    table.setPromocodesUsed(incrementIfTrue(event.isPromoCodeUsed()));
                    table.setBonusPointsPaid(event.getBonusPointsPaid());
                    table.setCreditsPaid(event.getCreditsPaid());
                    table.setMoneyPayed(event.getMoneyPaid());
                }
            });
        }
    }

    @Override
    protected <T> void preProcess(EventContent content, T event) throws SQLException {
        // No pre processing needed
    }

    @Override
    protected Class<PromoCode> getTableClass() {
        return PromoCode.class;
    }
}
