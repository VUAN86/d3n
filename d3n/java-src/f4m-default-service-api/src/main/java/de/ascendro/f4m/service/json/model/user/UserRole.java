package de.ascendro.f4m.service.json.model.user;

public enum UserRole {
	/**
	 * Default role for new connection and not authenticated user
	 */
	ANONYMOUS, 
	/**
	 * User is registered and logged in. We have the persons email so the person can play and store bonus points etc.
	 */
	REGISTERED,
	/**
	 * We have all required data. Can play all games but no money option - but can play for credits etc. if credits are earned or given as a prize / referral etc. to the player
	 */
	FULLY_REGISTERED,
	/**
	 * We have all the data plus a bank account and payment method exists. Can play all games including money options - but no gambling elements but he could be playing duels or tournaments if the game config - would be allowing playing UNDER 18 is allowed
	 */
	FULLY_REGISTERED_BANK,
	/**
	 * We have all the data plus the bank and the person is over 18. Can play all games including all that are marked over 18 - what I have to validate if the winning component is part of need to be over 18 or not
	 */
	FULLY_REGISTERED_BANK_O18,
	/**
	 * User is approved for community and can access WO of the Tenant
	 */
	COMMUNITY,
	/**
	 * User is Tenant Admin and can manage the Tenant, Apps and Games in Configurator
	 */
	ADMIN,
	/**
	 * User is Employee and can access Question Factory
	 */
	INTERNAL,
	/**
	 * User is External worker and can access Question Factory
	 */
	EXTERNAL,
	/**
	 * User with requested registration and not finished user identifier validation.
	 * Should be treated same as ANONYMOUS.
	 */
	NOT_VALIDATED;

    public static boolean isAnonymous(ClientInfo clientInfo) {
        return clientInfo.hasRole(clientInfo.getTenantId(), UserRole.ANONYMOUS.toString());
    }

	public static boolean isFullyRegistered(ClientInfo clientInfo) {
		return clientInfo.hasAnyRole(clientInfo.getTenantId(), FULLY_REGISTERED, FULLY_REGISTERED_BANK, FULLY_REGISTERED_BANK_O18);
	}
}
