package de.ascendro.f4m.service.usermessage.translation;

public class Messages {
    public static final String EMAIL_TEMPLATE_HEADER = "email.template.header";
    public static final String EMAIL_TEMPLATE_HEADER_DEFAULT_ADDRESSEE = "email.template.header.default.addressee";
    public static final String EMAIL_TEMPLATE_FOOTER = "email.template.footer";

	public static final String PAYMENT_SYSTEM_IDENTIFICATION_ERROR_PUSH = "payment.system.identification.error.push";
	public static final String PAYMENT_SYSTEM_IDENTIFICATION_SUCCESS_PUSH = "payment.system.identification.success.push";
	public static final String PAYMENT_SYSTEM_PAYMENT_ERROR_PUSH = "payment.system.payment.error.push";
	public static final String PAYMENT_SYSTEM_PAYMENT_SUCCESS_PUSH = "payment.system.payment.success.push";

    public static final String VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT = "voucher.won.assigned.to.user.subject";
    public static final String VOUCHER_WON_ASSIGNED_TO_USER_CONTENT = "voucher.won.assigned.to.user.content";
    public static final String VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL = "voucher.won.assigned.to.user.content.with.redemptionURL";
    public static final String VOUCHER_WON_IN_TOMBOLA_ASSIGNED_TO_USER_CONTENT = "voucher.won.in.tombola.assigned.to.user.content";
    public static final String VOUCHER_WON_IN_TOMBOLA_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL = "voucher.won.in.tombola.assigned.to.user.content.with.redemptionURL";
    public static final String VOUCHER_BOUGHT_ASSIGNED_TO_USER_SUBJECT = "voucher.bought.assigned.to.user.subject";
    public static final String VOUCHER_BOUGHT_ASSIGNED_TO_USER_CONTENT = "voucher.bought.assigned.to.user.content";
    public static final String VOUCHER_BOUGHT_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL = "voucher.bought.assigned.to.user.content.with.redemptionURL";

    public static final String GAME_DUEL_WON_PUSH = "game.duel.won.push";
    public static final String BUDDY_GAME_DUEL_WON_PUSH = "buddy.game.duel.won.push";
    public static final String GAME_DUEL_LOST_PUSH = "game.duel.lost.push";
    public static final String BUDDY_GAME_DUEL_LOST_PUSH = "buddy.game.duel.lost.push";
    
    public static final String FRIEND_PLAYER_ADDED_TO_GROUP_PUSH = "friend.player.added.to.group";

    public static final String GAME_INVITATION_PUSH = "game.invitation.push";
	public static final String GAME_DUEL_ONEHOUREXPIRE_INVITATION_PUSH = "game.duel.onehourexpire.push";
	public static final String GAME_TOURNAMENT_ONEHOUREXPIRE_INVITATION_PUSH = "game.tournament.onehourexpire.push";
    public static final String GAME_INVITATION_RESPONSE_ACCEPTED_PUSH = "game.invitation.response.accepted.push";
    public static final String GAME_INVITATION_RESPONSE_REJECTED_PUSH = "game.invitation.response.rejected.push";
    public static final String GAME_STARTING_SOON_PUSH = "game.starting.soon.push";
    public static final String GAME_PLAYER_READINESS_PUSH = "game.player.readiness.push";
    public static final String GAME_ENDED_PUSH = "game.ended.push";
    public static final String GAME_TOURNAMENT_WON_PUSH = "game.tournament.won.push";
    public static final String GAME_TOURNAMENT_LOST_PUSH = "game.tournament.lost.push";
	public static final String GAME_TOURNAMENT_ONE_HOUR_PUSH = "game.tournament.onehourwarning.push";
	public static final String GAME_TOURNAMENT_FIVE_MINUTES_PUSH = "game.tournament.fiveminwarning.push";

    public static final String TOMBOLA_DRAW_WIN_PUSH = "tombola.draw.win.push";
    public static final String TOMBOLA_DRAW_LOSE_PUSH = "tombola.draw.lose.push";
    public static final String TOMBOLA_DRAW_ANNOUNCEMENT_PUSH = "tombola.draw.announcement.push";
    public static final String TOMBOLA_OPEN_ANNOUNCEMENT_PUSH = "tombola.open.announcement.push";
    public static final String TOMBOLA_DRAW_EMAIL_SUBJECT = "tombola.draw.email.subject";
    public static final String TOMBOLA_DRAW_WIN_EMAIL_CONTENT = "tombola.draw.win.email.content";
    public static final String TOMBOLA_DRAW_CONSOLATION_EMAIL_CONTENT = "tombola.draw.consolation.email.content";
    public static final String TOMBOLA_DRAW_LOSE_EMAIL_CONTENT = "tombola.draw.lose.email.content";
    public static final String TOMBOLA_PRIZE_PAYOUT_ERROR_TO_ADMIN_SUBJECT = "tombola.prize.payout.error.to.admin.subject";
    public static final String TOMBOLA_PRIZE_PAYOUT_ERROR_TO_ADMIN_CONTENT = "tombola.prize.payout.error.to.admin.content";

    public static final String CURRENCY_POINTS = "currency.points";
    public static final String CURRENCY_CREDITS = "currency.credits";

    public static final String AMQ_MEMORY_ERROR_TO_ADMIN_SUBJECT = "amq.memory.error.to.admin.subject";
    public static final String AMQ_EXPIRED_MESSAGES_ERROR_TO_ADMIN_SUBJECT = "amq.expired.messages.error.to.admin.subject";
    public static final String AMQ_NO_CONSUMER_ERROR_TO_ADMIN_SUBJECT = "amq.noconsumer.error.to.admin.subject";


    public static final String MONTHLY_INVITES_PRIZE_PAYOUT_PUSH = "monthly.invites.prize.payout.push";
    public static final String MONTHLY_INVITES_PRIZE_PAYOUT_ERROR_TO_ADMIN_SUBJECT = "monthly.invites.prize.payout.error.to.admin.subject";
    public static final String MONTHLY_INVITES_PRIZE_PAYOUT_ERROR_TO_ADMIN_CONTENT = "monthly.invites.prize.payout.error.to.admin.content";
    public static final String REGISTRATION_BONUS_PRIZE_PAYOUT_PUSH_INITIAL = "registration.bonus.payout.push.initial";
    public static final String REGISTRATION_BONUS_PRIZE_PAYOUT_PUSH_FULL = "registration.bonus.payout.push.full";
    public static final String REGISTRATION_BONUS_PRIZE_PAYOUT_ERROR_TO_ADMIN_SUBJECT = "registration.bonus.payout.error.to.admin.subject";
    public static final String REGISTRATION_BONUS_PRIZE_PAYOUT_ERROR_TO_ADMIN_CONTENT = "registration.bonus.payout.error.to.admin.content";


    private Messages() {
		//only static usage of class intended
	}
}
