package de.ascendro.f4m.service.analytics.module.notification.util;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import javax.inject.Inject;

import de.ascendro.f4m.server.analytics.EventContent;
import de.ascendro.f4m.server.analytics.model.MultiplayerGameEndEvent;
import de.ascendro.f4m.server.friend.CommonBuddyElasticDao;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDao;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.TombolaTicket;
import de.ascendro.f4m.service.tombola.model.UserTombolaInfo;

public class NotificationUtil {

    private final TombolaAerospikeDao tombolaAerospikeDaoImpl;
    private final CommonBuddyElasticDao buddyElasticDao;

    @Inject
    public NotificationUtil(TombolaAerospikeDao tombolaAerospikeDaoImpl,
                            CommonBuddyElasticDao buddyElasticDao) {
        this.tombolaAerospikeDaoImpl = tombolaAerospikeDaoImpl;
        this.buddyElasticDao = buddyElasticDao;
    }

    public boolean isBuddyGame(MultiplayerGameEndEvent event, EventContent content) {
        return buddyElasticDao.isMyBuddy(content.getUserId(), event.getOpponentId());
    }

    public Set<TombolaResult> getTombolaResults(Long tombolaId) {
        Map<String, TombolaResult> results = new HashMap<>();
        if (tombolaId != null) {
            final Tombola tombola = tombolaAerospikeDaoImpl.getTombola(String.valueOf(tombolaId));
            int purchasedTicketsNumber = tombolaAerospikeDaoImpl.getPurchasedTicketsNumber(tombola.getId());
            //Adding results for all tombola tickets
            IntStream.range(0, purchasedTicketsNumber).forEach(ticketId -> addResultsForTicket(tombola, ticketId, results));
        }

        return new HashSet<>(results.values());
    }

    private void addResultsForTicket(Tombola tombola, int ticketId, Map<String, TombolaResult> results) {
        TombolaTicket tombolaTicket = tombolaAerospikeDaoImpl.getTombolaTicket(tombola.getId(), String.valueOf(ticketId));
        String userId = tombolaTicket != null ? tombolaTicket.getUserId() : null;
        if (userId != null && !results.containsKey(userId)){
            UserTombolaInfo userTombolaInfo = tombolaAerospikeDaoImpl.getUserTombola(userId, tombola.getId());
            if (userTombolaInfo != null) {
                TombolaResult tombolaResult = new TombolaResult(userId, false);
                if (userTombolaInfo.getTotalPrizesWon() > 0) {
					tombolaResult.setWinner(true);
					userTombolaInfo.getPrizes().forEach(prize -> tombolaResult.addPrizeName(prize.getName()));
				}
                results.put(userId, tombolaResult);
            }
        }
    }

    public Set<String> getTombolaParticipants(Long tombolaId) {
        Set<String> participants = new HashSet<>();
        if (tombolaId != null) {
            final Tombola tombola = tombolaAerospikeDaoImpl.getTombola(String.valueOf(tombolaId));
            int purchasedTicketsNumber = tombolaAerospikeDaoImpl.getPurchasedTicketsNumber(tombola.getId());

            IntStream.range(0, purchasedTicketsNumber).forEach(ticketId ->
                    participants.add(tombolaAerospikeDaoImpl.getTombolaTicket(tombola.getId(), String.valueOf(ticketId)).getUserId()));
        }
        return participants;
    }


}
