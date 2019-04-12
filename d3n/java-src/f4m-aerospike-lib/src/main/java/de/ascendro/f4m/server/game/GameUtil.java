package de.ascendro.f4m.server.game;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Set;

import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.game.engine.exception.F4MGameNotAvailableException;
import de.ascendro.f4m.service.game.selection.model.game.EntryFee;
import de.ascendro.f4m.service.game.selection.model.game.Game;
import de.ascendro.f4m.service.game.selection.model.game.GameType;
import de.ascendro.f4m.service.game.selection.model.multiplayer.CustomGameConfig;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserPermission;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.DateTimeUtil;

public class GameUtil {

	private GameUtil() {
		// private constructor to hide the public one
	}

	public static void validateIfGameAvailable(ZonedDateTime startDate, ZonedDateTime endDate, String gameId) {
		if (!DateTimeUtil.isCurrentBetween(startDate, endDate)) {
			throw new F4MGameNotAvailableException(String.format(
					"Game [%s] cannot be registered as it is not available; start date time [%s], end date time [%s]",
					gameId, startDate, endDate));
		}
	}
	
	public static boolean isGameAvailable(CustomGameConfig customGameConfig){
		return DateTimeUtil.isCurrentBetween(customGameConfig.getStartDateTime(), customGameConfig.getEndDateTime());
	}
	
	public static boolean isGameInvitationExpired(CustomGameConfig customGameConfig) {
		boolean result = false;
		if (customGameConfig.getExpiryDateTime() != null) {
			result = customGameConfig.getExpiryDateTime().isBefore(DateTimeUtil.getCurrentDateTime());
		}
		return result;
	}

	public static boolean isFreeGame(Game game, CustomGameConfig customGameConfig){
		boolean freeGame = game.isFree();
		if (customGameConfig != null && game.isEntryFeeDecidedByPlayer()) {
			freeGame = customGameConfig.isFree();
		}
		return freeGame;
	}
	
	public static void validateUserPermissionsForEntryFee(EntryFee entryFee, Set<String> permissions) {
		if (!entryFee.isFree() && entryFee.getEntryFeeCurrency() != null) {
			UserPermission entryFeeRequiredPermission = currencyToPermission(entryFee.getEntryFeeCurrency());
			if (!permissions.contains(entryFeeRequiredPermission.name())) {
				throw new F4MInsufficientRightsException(
						String.format("Missing game entry fee required permission [%s] for currency [%s]",
								entryFeeRequiredPermission, entryFee.getEntryFeeCurrency()));
			}
		}
	}

	private static UserPermission currencyToPermission(Currency currency) {
		UserPermission permission;
		if (currency == Currency.BONUS) {
			permission = UserPermission.GAME;
		} else if (currency == Currency.CREDIT) {
			permission = UserPermission.GAME_CREDIT;
		} else if (currency == Currency.MONEY) {
			permission = UserPermission.GAME_MONEY;
		} else {
			throw new F4MValidationFailedException("Invalid entry fee currency: " + currency);
		}
		return permission;
	}
	
	public static void validateUserAgeForEntryFee(EntryFee entryFee, Profile profile) {
		if (isEntryFeeRestrictedByAge(entryFee) && !profile.isOver18()) {
			throw new F4MInsufficientRightsException("User must be over 18 years old");
		}
	}

	private static boolean isEntryFeeRestrictedByAge(EntryFee entryFee) {
		return (entryFee.getEntryFeeCurrency() == Currency.MONEY)
				&& !entryFee.isFree();
	}
	
	/**
	 * For multiple purchases, I need to check if 
	 * 
	 * */
	public static boolean haveMultipleGamesPurchase(Game game) {
		return game.getEntryFeeBatchSize() > 1;
	}
	
	
	/**
	 * Calculate timestamp of the game instance expiration, when game play cannot be continued
	 * @param maxGamePlayHours - hours of max game play (e.g. 24h, @see de.ascendro.f4m.server.config.GameConfigImpl.GAME_MAX_PLAY_TIME_IN_HOURS_DEFAULT)
	 * @param gameEndDate - game end date
	 * @return min of game end date and max game play timestamps
	 */
	public static long getGamePlayExpirationTimestamp(long maxGamePlayHours, ZonedDateTime gameEndDate){
		final long maxGamePlayTimestamp = DateTimeUtil.getCurrentDateTime()
				.plusHours(maxGamePlayHours)
				.toInstant().toEpochMilli();

		final long gameEndTimestamp = gameEndDate.toInstant().toEpochMilli();

		return Math.min(maxGamePlayTimestamp, gameEndTimestamp);
	}
	
	public static void validateUserRolesForGameType(GameType gameType, ClientInfo clientInfo) {
		if (gameType.isMultiUser() && !UserRole.isFullyRegistered(clientInfo)) {
			throw new F4MInsufficientRightsException(
					String.format("Insufficient roles %s for game type [%s]", Arrays.toString(clientInfo.getRoles()), gameType.name()));
		}
	}

}
