package de.ascendro.f4m.service.usermessage.onesignal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.usermessage.translation.Messages;

public class PushNotificationTypeMessageMapperTest {

	private PushNotificationTypeMessageMapper mapper = new PushNotificationTypeMessageMapper();

	@Test
	public void testIsMessageEnabled() {
		Profile profile = new Profile();
		profile.setPushNotificationsDuel(false);
		assertFalse(mapper.isMessageEnabled(Messages.GAME_DUEL_ONEHOUREXPIRE_INVITATION_PUSH, profile));
		profile.setPushNotificationsDuel(true);
		assertTrue(mapper.isMessageEnabled(Messages.GAME_DUEL_ONEHOUREXPIRE_INVITATION_PUSH, profile));
	}
	
	@Test
	public void testIsMessageEnabledIfUnspecified() {
		Profile profile = new Profile();
		assertTrue(mapper.isMessageEnabled(Messages.GAME_DUEL_ONEHOUREXPIRE_INVITATION_PUSH, profile));
	}
}
