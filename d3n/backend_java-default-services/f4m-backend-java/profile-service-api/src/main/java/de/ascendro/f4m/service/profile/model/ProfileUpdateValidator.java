package de.ascendro.f4m.service.profile.model;

import java.time.ZonedDateTime;
import java.util.Comparator;

public class ProfileUpdateValidator {

	public static boolean isProfileUpdateRelevantForPayment(Profile oldProfile, Profile updatedProfile) {
		//This check if user change should be sent to payment service, could be implemented in payment service
		//but it cannot be done because Profile is needed here, and placing it in payment-api would create dependency loop.
		Comparator<String> stringCmp = Comparator.nullsFirst(String::compareTo);
		Comparator<ZonedDateTime> dateCmp = Comparator.nullsFirst(ZonedDateTime::compareTo);
		Comparator<ProfileUser> personComparator = Comparator.nullsFirst(Comparator
				.comparing(ProfileUser::getFirstName, stringCmp)
				.thenComparing(ProfileUser::getLastName, stringCmp)
				.thenComparing(ProfileUser::getBirthDate, dateCmp));
		Comparator<ProfileAddress> addressComparator = Comparator
				.nullsFirst(Comparator.comparing(ProfileAddress::getStreet, stringCmp)
						.thenComparing(ProfileAddress::getPostalCode, stringCmp));
		return personComparator.compare(oldProfile.getPersonWrapper(), updatedProfile.getPersonWrapper()) != 0
				|| addressComparator.compare(oldProfile.getAddress(), updatedProfile.getAddress()) != 0;
	}
}
