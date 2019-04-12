package de.ascendro.f4m.service.payment.manager;

import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;

public final class PaymentUserIdCalculator {
	private static final String TENANT_PROFILE_SEPARATOR = "_";

	private PaymentUserIdCalculator() {
		//hidden constructor to make sonar happy 
	}

	public static String calcPaymentUserId(String tenantId, String profileId) {
		if (tenantId.contains(TENANT_PROFILE_SEPARATOR)) {
			throw new F4MValidationFailedException("Incorrect tenantId");
		}
		if (profileId != null && profileId.contains(TENANT_PROFILE_SEPARATOR)) {
			throw new F4MValidationFailedException("Incorrect profileId '" + profileId + "'");
		}
		StringBuilder sb = new StringBuilder();
		sb.append(tenantId);
		sb.append(TENANT_PROFILE_SEPARATOR);
		if (profileId != null) {
			sb.append(profileId);
		}
		return sb.toString();
	}

	public static String calcTenantIdFromUserId(String userId) {
		String tenantId = userId;
		if (userId.contains(TENANT_PROFILE_SEPARATOR)) {
			tenantId = userId.substring(0, userId.indexOf(TENANT_PROFILE_SEPARATOR));
		}
		return tenantId;
	}

	public static String calcProfileIdFromUserId(String userId) {
		String profileId = null;
		if (userId.contains(TENANT_PROFILE_SEPARATOR)) {
			profileId = userId.substring(userId.indexOf(TENANT_PROFILE_SEPARATOR) + 1);
			if (profileId.length() == 0) {
				profileId = null;
			}
		}
		return profileId;
	}

	public static boolean isPaymentUserIdWithProfileId(String userId) {
		boolean separatorFound = false;
		if (userId != null) {
			int separatorLocation = userId.indexOf(TENANT_PROFILE_SEPARATOR);
			separatorFound = separatorLocation > 0 && separatorLocation < userId.length() - 1;
			if (separatorFound) {
				separatorFound = userId.indexOf(TENANT_PROFILE_SEPARATOR, separatorLocation + 1) == -1;
			}
		}
		return separatorFound;
	}

	public static boolean isDestinationTenantAccount(String profileId) {
		return profileId == null;
	}
}
