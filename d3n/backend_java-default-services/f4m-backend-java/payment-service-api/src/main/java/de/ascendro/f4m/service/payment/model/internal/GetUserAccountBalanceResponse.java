package de.ascendro.f4m.service.payment.model.internal;

import java.util.List;

import de.ascendro.f4m.service.json.model.JsonMessageContent;

public class GetUserAccountBalanceResponse implements JsonMessageContent {
	private List<GetAccountBalanceResponse> balances;

    public GetUserAccountBalanceResponse() {
    }

    public GetUserAccountBalanceResponse(List<GetAccountBalanceResponse> balances) {
        this.balances = balances;
    }

    public List<GetAccountBalanceResponse> getBalances() {
		return balances;
	}

	public void setBalances(List<GetAccountBalanceResponse> balances) {
		this.balances = balances;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GetUserAccountBalanceResponse [balances=");
		builder.append(balances);
		builder.append("]");
		return builder.toString();
	}
}
