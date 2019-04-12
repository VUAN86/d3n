package de.ascendro.f4m.service.profile.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProfileUpdateValidatorTest {
	
	@Test
	public void areEmptyProfilesEqual() throws Exception {
		Profile oldProfile = new Profile();
		Profile updatedProfile = new Profile();
		assertFalse(ProfileUpdateValidator.isProfileUpdateRelevantForPayment(oldProfile, updatedProfile));
	}
	
	@Test
	public void areProfilesWithNullValuesNotEqual() throws Exception {
		Profile oldProfile = createFullProfile();
		//oldProfile.getAddress().setStreet(null);
		Profile updatedProfile = createFullProfile();
		updatedProfile.getAddress().setStreet(null);
		assertTrue(ProfileUpdateValidator.isProfileUpdateRelevantForPayment(oldProfile, updatedProfile));
	}

	@Test
	public void areProfilesWithDifferentValuesNotEqual() throws Exception {
		Profile oldProfile = createFullProfile();
		oldProfile.getAddress().setStreet("Different");
		Profile updatedProfile = createFullProfile();
		assertTrue(ProfileUpdateValidator.isProfileUpdateRelevantForPayment(oldProfile, updatedProfile));
	}

	@Test
	public void areProfilesEqual() throws Exception {
		Profile oldProfile = createFullProfile();
		Profile updatedProfile = createFullProfile();
		assertFalse(ProfileUpdateValidator.isProfileUpdateRelevantForPayment(oldProfile, updatedProfile));
	}

	private Profile createFullProfile() {
		Profile oldProfile = new Profile();
		oldProfile.setPersonWrapper(createProfileUser());
		oldProfile.setAddress(createProfileAddress());
		return oldProfile;
	}

	private ProfileUser createProfileUser() {
		ProfileUser profileUser = new ProfileUser();
		profileUser.setFirstName("F");
		profileUser.setLastName("L");
		profileUser.setBirthDate("2017-01-01T01:02:01Z");
		return profileUser;
	}
	
	private ProfileAddress createProfileAddress() {
		ProfileAddress address = new ProfileAddress();
		address.setStreet("S");
		address.setPostalCode("X");
		return address;
	}

}
