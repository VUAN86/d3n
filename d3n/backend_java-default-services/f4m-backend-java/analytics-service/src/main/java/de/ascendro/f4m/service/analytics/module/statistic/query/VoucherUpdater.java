package de.ascendro.f4m.service.analytics.module.statistic.query;

import java.sql.SQLException;

import org.slf4j.Logger;

import com.google.inject.Inject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.model.VoucherCountEvent;
import de.ascendro.f4m.server.analytics.model.VoucherUsedEvent;
import de.ascendro.f4m.service.analytics.logging.InjectLogger;
import de.ascendro.f4m.service.analytics.module.statistic.model.Voucher;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.BaseUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.ITableUpdater;
import de.ascendro.f4m.service.analytics.module.statistic.query.base.Renderer;
import de.ascendro.f4m.service.analytics.notification.NotificationCommon;
import de.ascendro.f4m.service.config.Config;


public class VoucherUpdater extends BaseUpdater<Voucher> implements ITableUpdater<Voucher> {
    @InjectLogger
    private static Logger LOGGER;

    @Inject
    public VoucherUpdater(Config config, NotificationCommon notificationUtil) {
        super(config, notificationUtil);
    }

    @Override
    public void processEvent(EventContent content) throws Exception {
        if (content.isOfType(RewardEvent.class)) {
            process(content, new Renderer<Voucher, RewardEvent>() {
                @Override
                public void render(Voucher table, RewardEvent event) {
                    table.setId(event.getVoucherId());
                    table.setWon(incrementIfTrue(event.isVoucherWon()));
                    table.setSpecialPrizes(incrementIfTrue(event.isSuperPrizeWon()));
                }
            });
        } else if (content.isOfType(VoucherUsedEvent.class)) {
            process(content, new Renderer<Voucher, VoucherUsedEvent>() {
                @Override
                public void render(Voucher table, VoucherUsedEvent event) {
                    table.setId(event.getVoucherId());
                    table.setArchived(1);
                }
            });
        } else if (content.isOfType(VoucherCountEvent.class)) {
            process(content, new Renderer<Voucher, VoucherCountEvent>() {
                @Override
                public void render(Voucher table, VoucherCountEvent event) {
                    table.setId(event.getVoucherId());
                    table.setInventoryCount(event.getVoucherCount());
                }
            });
        }
    }

    @Override
    protected Class<Voucher> getTableClass() {
        return Voucher.class;
    }

    @Override
    protected <T> void preProcess(EventContent content, T event) throws SQLException {
        // No pre processing needed
    }

    @Override
    public String toString() {
        return "VoucherUpdater{" +
                "config=" + config +
                ", batchSize=" + batchSize +
                ", updateTrigger=" + updateTrigger +
                "} " + super.toString();
    }
}
