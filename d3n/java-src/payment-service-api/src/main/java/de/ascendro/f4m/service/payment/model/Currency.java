package de.ascendro.f4m.service.payment.model;

/**
 * Enumeration representing currencies defined in F4M.
 */
public enum Currency {
	MONEY("Money"),
	BONUS("Bonus points"),
	CREDIT("Credits");
	
	private final String fullName;
		
	private Currency(String fullName) {
		this.fullName = fullName;
	}

	public String getFullName() {
		return fullName;
	}
	
}
