package de.ascendro.f4m.server.game;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.Sets;

import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.game.engine.exception.F4MGameNotAvailableException;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserPermission;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class GameUtilTest {

	public static final String GAME_ID = "2b551c1e-a718-11e6-80f5-76304dec7eb7";
	public static final String MGI_ID = "60551c1e-a718-11e6-555-76304dec7eb7";
	
	public static final ZonedDateTime START_DATE = DateTimeUtil.getCurrentDateStart();
	public static final ZonedDateTime END_DATE = DateTimeUtil.getCurrentDateEnd();
	
	@Test
	public void testValidateIfGameAvailableForSinglePlayer() {
		final CustomGameConfig game = mock(CustomGameConfig.class);
		when(game.getGameId()).thenReturn(GAME_ID);
		
		//valid
		when(game.getStartDateTime()).thenReturn(START_DATE);
		when(game.getEndDateTime()).thenReturn(END_DATE);
		GameUtil.validateIfGameAvailable(game.getStartDateTime(), game.getEndDateTime(), game.getGameId());
		
		//invalid
		when(game.getStartDateTime()).thenReturn(END_DATE);
		when(game.getEndDateTime()).thenReturn(START_DATE);
		try {
			GameUtil.validateIfGameAvailable(game.getStartDateTime(), game.getEndDateTime(), game.getGameId());
			fail("Expected expired game for dates [" + game.getStartDateTime() + "," + game.getEndDateTime() + "]");
		} catch (F4MGameNotAvailableException gameExpiredEx) {
			// Expected exception
		} catch (Exception ex) {
			fail("Unexpected expired game exception type: " + ex.getClass().getSimpleName());
		}
	}
	
	@Test
	public void testValidateIfGameAvailableForMultiplayer() {
		final CustomGameConfig game = mock(CustomGameConfig.class);
		when(game.getGameId()).thenReturn(GAME_ID);
		when(game.getStartDateTime()).thenReturn(START_DATE);
		when(game.getEndDateTime()).thenReturn(END_DATE);
		
		//valid
		GameUtil.validateIfGameAvailable(game.getStartDateTime(), game.getEndDateTime(), game.getGameId());
		
		//invalid: custom dates
		when(game.getStartDateTime()).thenReturn(START_DATE);
		when(game.getEndDateTime()).thenReturn(START_DATE);
		try {
			GameUtil.validateIfGameAvailable(game.getStartDateTime(), game.getEndDateTime(), game.getGameId());
			fail("Expected expired game for dates [" + game.getStartDateTime() + "," + game.getEndDateTime() + "]");
		} catch (F4MGameNotAvailableException gameExpiredEx) {
			// Expected exception
		} catch (Exception ex) {
			fail("Unexpected expired game exception type: " + ex.getClass().getSimpleName());
		}
	}
	
	@Test
	public void testIsGameInvitationExpired() {
		CustomGameConfig customGameConfig = mock(CustomGameConfig.class);
		ZonedDateTime now = DateTimeUtil.getCurrentDateTime();
		
		// valid
		when(customGameConfig.getExpiryDateTime()).thenReturn(now.plusMinutes(5)); // valid for five minutes
		assertFalse(GameUtil.isGameInvitationExpired(customGameConfig));
		
		// invalid
		when(customGameConfig.getExpiryDateTime()).thenReturn(now.minusMinutes(5)); // expired for five minutes
		assertTrue(GameUtil.isGameInvitationExpired(customGameConfig));
	}
	
	@Test
	public void validateUserPermissionsForEntryFee() {
		EntryFee entryFee = mock(EntryFee.class);
		when(entryFee.isFree()).thenReturn(true);
		GameUtil.validateUserPermissionsForEntryFee(entryFee, Sets.newHashSet("any_user_permission"));
		
		when(entryFee.isFree()).thenReturn(false);
		
		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.BONUS);
		GameUtil.validateUserPermissionsForEntryFee(entryFee, Sets.newHashSet(UserPermission.GAME.name()));
		
		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.CREDIT);
		GameUtil.validateUserPermissionsForEntryFee(entryFee, Sets.newHashSet(UserPermission.GAME_CREDIT.name()));
		
		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.MONEY);
		GameUtil.validateUserPermissionsForEntryFee(entryFee, Sets.newHashSet(UserPermission.GAME_MONEY.name()));
	}
	
	@Test
	public void validateInsuffucientUserPermissionsForEntryFee() {
		EntryFee entryFee = mock(EntryFee.class);
		when(entryFee.isFree()).thenReturn(false);

		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.BONUS);
		try {
			GameUtil.validateUserPermissionsForEntryFee(entryFee, Sets.newHashSet(UserPermission.GAME_CREDIT.name(), UserPermission.GAME_MONEY.name()));
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException irEx) {
		}

		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.CREDIT);
		try {
			GameUtil.validateUserPermissionsForEntryFee(entryFee, Sets.newHashSet(UserPermission.GAME.name(), UserPermission.GAME_MONEY.name()));
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException irEx) {
		}

		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.MONEY);
		try {
			GameUtil.validateUserPermissionsForEntryFee(entryFee, Sets.newHashSet(UserPermission.GAME.name(), UserPermission.GAME_CREDIT.name()));
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException irEx) {
		}
	}
	
	@Test
	public void testValidateUserAgeForEntryFee() throws Exception {
		Profile under18 = getTestProfile(15);
		Profile over18 = getTestProfile(79);

		CustomGameConfig entryFee = mock(CustomGameConfig.class);
		when(entryFee.isFree()).thenReturn(false);

		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.BONUS);
		GameUtil.validateUserAgeForEntryFee(entryFee, under18);
		GameUtil.validateUserAgeForEntryFee(entryFee, over18);

		when(entryFee.getEntryFeeCurrency()).thenReturn(Currency.MONEY);
		try {
			GameUtil.validateUserAgeForEntryFee(entryFee, under18);
			fail("F4MInsufficientRightsException must be thrown");
		} catch (F4MInsufficientRightsException e) {
			assertThat(e.getMessage(), equalTo("User must be over 18 years old"));
		}

		GameUtil.validateUserAgeForEntryFee(entryFee, over18);
	}
	
	private Profile getTestProfile(int age) {
		ProfileUser person = new ProfileUser();
		person.setBirthDate(DateTimeUtil.getCurrentDateStart().minusYears(age));
		Profile profile = new Profile();
		profile.setPersonWrapper(person);
		return profile;
	}
	
	@Test
	public void testGetGamePlayExpirationTimestamp(){
		//min(12h, 24h) -> 12h as timestamp
		assertThat(GameUtil.getGamePlayExpirationTimestamp(12, DateTimeUtil.getCurrentDateTime().plusDays(1)),
				lessThanOrEqualTo(DateTimeUtil.getCurrentDateTime().plusHours(12).toInstant().toEpochMilli()));
		//min(24g, 5h) -> 5h as timestamp
		assertThat(GameUtil.getGamePlayExpirationTimestamp(24, DateTimeUtil.getCurrentDateTime().plusHours(5)),
				lessThanOrEqualTo(DateTimeUtil.getCurrentDateTime().plusHours(5).toInstant().toEpochMilli()));
	}
	

	@Test
	public void testValidateUserRolesForGameType() throws Exception {
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setRoles(UserRole.ADMIN.name(), UserRole.REGISTERED.name());
		try {
			GameUtil.validateUserRolesForGameType(GameType.DUEL, clientInfo);
			fail("F4MInsufficientRightsException expected");
		} catch (F4MInsufficientRightsException e) {
		}

		clientInfo.setRoles(UserRole.REGISTERED.name(), UserRole.FULLY_REGISTERED.name());
		try {
			GameUtil.validateUserRolesForGameType(GameType.DUEL, clientInfo);
		} catch (Exception e) {
			fail(String.format("Unexpected exception [%s]", e.getMessage()));
		}
	}

}
