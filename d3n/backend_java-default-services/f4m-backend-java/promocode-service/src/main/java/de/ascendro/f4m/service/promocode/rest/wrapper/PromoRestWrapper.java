package de.ascendro.f4m.service.promocode.rest.wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PromoRestWrapper extends RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(PromoRestWrapper.class);

	public PromoResponseType checkPromoStatus(String promo) {
		PromoResponseType result = PromoResponseType.fromString(callGet(promo));
		LOGGER.info("Returned result {}", result.getValue());
		return result;
	}
}