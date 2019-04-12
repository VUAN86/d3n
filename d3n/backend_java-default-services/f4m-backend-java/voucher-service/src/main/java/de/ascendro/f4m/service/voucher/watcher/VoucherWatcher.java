package de.ascendro.f4m.service.voucher.watcher;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.ascendro.f4m.service.voucher.util.VoucherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VoucherWatcher {

    private VoucherUtil voucherUtil;

    private final Logger LOGGER = LoggerFactory.getLogger(VoucherWatcher.class);

    @SuppressWarnings("rawtypes")
    private final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("VoucherWatcher").build());

    @Inject
    public VoucherWatcher(VoucherUtil voucherUtil) {
        this.voucherUtil=voucherUtil;
    }

    public void startWatcher() {
        LOGGER.info("Statistic watcher started");
        service.scheduleAtFixedRate(() -> {
            LOGGER.debug("WORK");
            try {
                voucherUtil.deleteExpiredVoucherListEntries("3");
            } catch (Exception e) {
                LOGGER.error("startWatcher EX:{}", e);
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    public void stopWatcher() {
        if (!service.isShutdown()) {
            service.shutdownNow();
            LOGGER.info("Statistic watcher stopped");
        }
    }
}
