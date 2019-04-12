package de.ascendro.f4m.service.profile.model.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.ascendro.f4m.service.profile.model.Profile;

public class ApiProfileTest {
	@Test
	public void testProfileDateUpdatesApiProfile() throws Exception {
		Profile profile = new Profile();
		profile.setPhotoId("PhotoId");
		
		ApiProfile apiProfile = new ApiProfile(profile);
		assertEquals("PhotoId", apiProfile.getProfilePhotoId());
	}
}
