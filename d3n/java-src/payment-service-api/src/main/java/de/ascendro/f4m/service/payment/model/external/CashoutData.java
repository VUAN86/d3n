package de.ascendro.f4m.service.payment.model.external;

public class CashoutData {
	private String beneficiary;
	private String iban;
	private String bic;

	public String getBeneficiary() {
		return beneficiary;
	}

	public void setBeneficiary(String beneficiary) {
		this.beneficiary = beneficiary;
	}

	public String getIban() {
		return iban;
	}

	public void setIban(String iban) {
		this.iban = iban;
	}

	public String getBic() {
		return bic;
	}

	public void setBic(String bic) {
		this.bic = bic;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CashoutData [beneficiary=");
		builder.append(beneficiary);
		builder.append(", iban=");
		builder.append(iban);
		builder.append(", bic=");
		builder.append(bic);
		builder.append("]");
		return builder.toString();
	}
}
