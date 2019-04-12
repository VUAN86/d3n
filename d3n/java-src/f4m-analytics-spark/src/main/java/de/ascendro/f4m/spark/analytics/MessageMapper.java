package de.ascendro.f4m.spark.analytics;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.ascendro.f4m.spark.analytics.model.AdMessage;
import de.ascendro.f4m.spark.analytics.model.BaseEventMessage;
import de.ascendro.f4m.spark.analytics.model.GameEndMessage;
import de.ascendro.f4m.spark.analytics.model.InviteMessage;
import de.ascendro.f4m.spark.analytics.model.InvoiceMessage;
import de.ascendro.f4m.spark.analytics.model.MultiplayerGameEndMessage;
import de.ascendro.f4m.spark.analytics.model.PaymentMessage;
import de.ascendro.f4m.spark.analytics.model.PlayerGameEndMessage;
import de.ascendro.f4m.spark.analytics.model.PromoCodeMessage;
import de.ascendro.f4m.spark.analytics.model.RewardMessage;
import de.ascendro.f4m.spark.analytics.model.ShopInvoiceMessage;
import de.ascendro.f4m.spark.analytics.model.TombolaEndMessage;
import de.ascendro.f4m.spark.analytics.model.VoucherCountMessage;
import de.ascendro.f4m.spark.analytics.model.VoucherUsedMessage;

public class MessageMapper {

    private static final Set<Class<? extends BaseEventMessage>> EVENT_MESSAGE_SET;

    private MessageMapper(){}

    static {
        Set<Class<? extends BaseEventMessage>> result = new HashSet<>();
        result.add(AdMessage.class);
        result.add(GameEndMessage.class);
        result.add(InviteMessage.class);
        result.add(InvoiceMessage.class);
        result.add(ShopInvoiceMessage.class);
        result.add(MultiplayerGameEndMessage.class);
        result.add(PaymentMessage.class);
        result.add(PlayerGameEndMessage.class);
        result.add(PromoCodeMessage.class);
        result.add(RewardMessage.class);
        result.add(TombolaEndMessage.class);
        result.add(VoucherUsedMessage.class);
        result.add(VoucherCountMessage.class);
        EVENT_MESSAGE_SET = Collections.unmodifiableSet(result);
    }

    public static Set<Class<? extends BaseEventMessage>> getEventMessageSet() {
        return EVENT_MESSAGE_SET;
    }
}
