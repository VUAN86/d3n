package de.ascendro.f4m.service.payment.payment.system.repository.impl;


import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.payment.payment.system.model.GameBalance;
import de.ascendro.f4m.service.payment.payment.system.repository.GameBalanceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Objects;


public class GameBalanceRepositoryImpl implements GameBalanceRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameBalanceRepositoryImpl.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public GameBalance get(String mgiId, EntityManager entityManager) {
        LOGGER.debug("get mgiId {} ", mgiId);
        String sqlString =
                "SELECT * " +
                        "FROM game_balance gb " +
                        "WHERE gb.mgiId='" + mgiId + "';";
        Query query = entityManager.createNativeQuery(sqlString, GameBalance.class);
        return (GameBalance) query.getSingleResult();
    }

    @Override
    public void create(GameBalance gameBalance, EntityManager entityManager) {
        entityManager.persist(gameBalance);
    }

    @Override
    public GameBalance update(GameBalance gameBalance, EntityManager entityManager) {
        roundGameBalance(gameBalance);
        return entityManager.merge(gameBalance);
    }

    private void roundGameBalance(GameBalance gameBalance) {
        if (Objects.nonNull(gameBalance)) {
            if (gameBalance.getCurrency() == Currency.BONUS || gameBalance.getCurrency()== Currency.CREDIT) {
                gameBalance.setBalance(gameBalance.getBalance().setScale(0, RoundingMode.HALF_UP));
            }
        }
    }
}
