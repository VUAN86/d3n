package de.ascendro.f4m.server.analytics;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.ascendro.f4m.server.analytics.model.AdEvent;
import de.ascendro.f4m.server.analytics.model.GameEndEvent;
import de.ascendro.f4m.server.analytics.model.InviteEvent;
import de.ascendro.f4m.server.analytics.model.InvoiceEvent;
import de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.NewTombolaEvent;
import de.ascendro.f4m.server.analytics.model.PaymentEvent;
import de.ascendro.f4m.server.analytics.model.PlayerGameEndEvent;
import de.ascendro.f4m.server.analytics.model.PromoCodeEvent;
import de.ascendro.f4m.server.analytics.model.QuestionFactoryEvent;
import de.ascendro.f4m.server.analytics.model.RewardEvent;
import de.ascendro.f4m.server.analytics.model.ShopInvoiceEvent;
import de.ascendro.f4m.server.analytics.model.TombolaEndAnnouncementEvent;
import de.ascendro.f4m.server.analytics.model.TombolaEndEvent;
import de.ascendro.f4m.server.analytics.model.UserRegistrationEvent;
import de.ascendro.f4m.server.analytics.model.VoucherCountEvent;
import de.ascendro.f4m.server.analytics.model.VoucherUsedEvent;

public class EventsDestinationMapper {

    private static final Set<String> NOTIFICATION_SET;
    private static final Set<String> STATISTIC_SET;
    private static final Set<String> LIVE_MAP_SET;
    private static final Set<String> SPARK_SET;
    private static final Set<String> JOB_SET;

    private EventsDestinationMapper(){}

    static {
        Set<String> result = new HashSet<>();
        result.add(MultiplayerGameEndEvent.class.getCanonicalName());
        result.add(NewTombolaEvent.class.getCanonicalName());
        result.add(TombolaEndAnnouncementEvent.class.getCanonicalName());
        result.add(TombolaEndEvent.class.getCanonicalName());
        NOTIFICATION_SET =  Collections.unmodifiableSet(result);

        result = new HashSet<>();
        result.add(MultiplayerGameEndEvent.class.getCanonicalName());
        result.add(AdEvent.class.getCanonicalName());
        result.add(GameEndEvent.class.getCanonicalName());
        result.add(PaymentEvent.class.getCanonicalName());
        result.add(PlayerGameEndEvent.class.getCanonicalName());
        result.add(RewardEvent.class.getCanonicalName());
        result.add(PromoCodeEvent.class.getCanonicalName());
        result.add(VoucherUsedEvent.class.getCanonicalName());
        result.add(InviteEvent.class.getCanonicalName());
        result.add(VoucherCountEvent.class.getCanonicalName());
        STATISTIC_SET = Collections.unmodifiableSet(result);

        result = new HashSet<>();
        result.add(MultiplayerGameEndEvent.class.getCanonicalName());
        LIVE_MAP_SET = Collections.unmodifiableSet(result);

        result = new HashSet<>();
        result.add(MultiplayerGameEndEvent.class.getCanonicalName());
        result.add(AdEvent.class.getCanonicalName());
        result.add(GameEndEvent.class.getCanonicalName());
        result.add(PaymentEvent.class.getCanonicalName());
        result.add(PlayerGameEndEvent.class.getCanonicalName());
        result.add(PromoCodeEvent.class.getCanonicalName());
        result.add(RewardEvent.class.getCanonicalName());
        result.add(TombolaEndEvent.class.getCanonicalName());
        result.add(VoucherUsedEvent.class.getCanonicalName());
        result.add(InvoiceEvent.class.getCanonicalName());
        result.add(InviteEvent.class.getCanonicalName());
        result.add(VoucherCountEvent.class.getCanonicalName());
        result.add(ShopInvoiceEvent.class.getCanonicalName());
        SPARK_SET = Collections.unmodifiableSet(result);

        result = new HashSet<>();
        result.add(MultiplayerGameEndEvent.class.getCanonicalName());
        result.add(RewardEvent.class.getCanonicalName());
        result.add(PlayerGameEndEvent.class.getCanonicalName());
        result.add(InviteEvent.class.getCanonicalName());
        result.add(PromoCodeEvent.class.getCanonicalName());
        result.add(AdEvent.class.getCanonicalName());
        result.add(QuestionFactoryEvent.class.getCanonicalName());
        result.add(UserRegistrationEvent.class.getCanonicalName());
        JOB_SET = Collections.unmodifiableSet(result);
    }

    public static boolean isNotificationEvent(String eventClassCanonicalName) {
        return NOTIFICATION_SET.contains(eventClassCanonicalName);
    }

    public static boolean isStatisticEvent(String eventClassCanonicalName) {
        return STATISTIC_SET.contains(eventClassCanonicalName);
    }

    public static boolean isLiveMapEvent(String eventClassCanonicalName) {
        return LIVE_MAP_SET.contains(eventClassCanonicalName);
    }

    public static boolean isSparkEvent(String eventClassCanonicalName) {
        return SPARK_SET.contains(eventClassCanonicalName);
    }

    public static boolean isJobEvent(String eventClassCanonicalName) {
        return JOB_SET.contains(eventClassCanonicalName);
    }
}
