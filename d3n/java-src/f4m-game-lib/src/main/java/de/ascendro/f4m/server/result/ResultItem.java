package de.ascendro.f4m.server.result;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.util.F4MEnumUtils;

public class ResultItem extends JsonObjectWrapper {

	public static final String RESULT_TYPE_PROPERTY = "resultType";
	public static final String AMOUNT_PROPERTY = "amount";
	public static final String CURRENCY_PROPERTY = "currency";

	public ResultItem() {
		// Initialize empty object
	}

	public ResultItem(ResultType resultType, double amount) {
		this(resultType, amount, null);
	}
	
	public ResultItem(ResultType resultType, double amount, Currency currency) {
		setResultType(resultType);
		setAmount(amount);
		setCurrency(currency);
	}
	
	public ResultItem(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	/**
	 * Result type.
	 */
	public ResultType getResultType() {
		return ResultType.fromString(getPropertyAsString(RESULT_TYPE_PROPERTY));
	}

	public void setResultType(ResultType resultType) {
		setProperty(RESULT_TYPE_PROPERTY, resultType == null ? null : resultType.getValue());
	}
	
	/**
	 * Result amount.
	 */
	public double getAmount() {
		return getPropertyAsDouble(AMOUNT_PROPERTY);
	}

	public void setAmount(double amount) {
		setProperty(AMOUNT_PROPERTY, amount);
	}

	/** 
	 * Currency of result, if applicable, otherwise <code>null</code>.
	 */
	public Currency getCurrency() {
		return F4MEnumUtils.getEnum(Currency.class, getPropertyAsString(CURRENCY_PROPERTY));
	}

	public void setCurrency(Currency currency) {
		if (currency == null) {
			jsonObject.remove(CURRENCY_PROPERTY);
		} else {
			setProperty(CURRENCY_PROPERTY, currency.toString());
		}
	}

}
