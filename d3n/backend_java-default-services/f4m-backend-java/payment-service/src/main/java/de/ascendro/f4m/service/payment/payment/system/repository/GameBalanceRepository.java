package de.ascendro.f4m.service.payment.payment.system.repository;


import de.ascendro.f4m.service.payment.payment.system.model.GameBalance;

import javax.persistence.EntityManager;

public interface GameBalanceRepository   {

    GameBalance get(String mgiId, EntityManager entityManager);

    void create(GameBalance gameBalance, EntityManager entityManager);

    GameBalance update(GameBalance gameBalance, EntityManager entityManager);
}
