package de.ascendro.f4m.service.profile.util;

import javax.inject.Inject;

import de.ascendro.f4m.server.PrimaryKeyUtil;
import de.ascendro.f4m.service.config.Config;

public class EndConsumerInvoicePrimaryKeyUtil extends PrimaryKeyUtil<String> {

    private static final String EC_INVOICE_PK_PREFIX = "endConsumerInvoiceList";

    @Inject
    public EndConsumerInvoicePrimaryKeyUtil(Config config) {
        super(config);
    }

	public String createPrimaryKey(String tenantId, String userId) {
		return EC_INVOICE_PK_PREFIX + KEY_ITEM_SEPARATOR + tenantId + KEY_ITEM_SEPARATOR + userId;
	}
}
