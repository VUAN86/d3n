package de.ascendro.f4m.service.payment.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Describes object in Paydent/Expolio API.
 */
@XmlRootElement
public class MergeAccountsRest {

	public MergeAccountsRest() {
		setState("Merged");
	}
	
	@JsonProperty("State")
	protected String state;

	@JsonProperty("CurrencyId")
	protected Long currencyId;

	@JsonProperty("accountsToMerge")
	protected Long[] accountsToMerge;

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public Long getCurrencyId() {
		return currencyId;
	}

	public void setCurrencyId(Long currencyId) {
		this.currencyId = currencyId;
	}

	public Long[] getAccountsToMerge() {
		return accountsToMerge;
	}

	public void setAccountsToMerge(Long[] accountsToMerge) {
		this.accountsToMerge = accountsToMerge;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MergeAccountsRest [State=");
		builder.append(state);
		builder.append(", CurrencyId=");
		builder.append(currencyId);
		builder.append(", accountsToMerge=");
		builder.append(accountsToMerge);
		builder.append("]");
		return builder.toString();
	}
}
