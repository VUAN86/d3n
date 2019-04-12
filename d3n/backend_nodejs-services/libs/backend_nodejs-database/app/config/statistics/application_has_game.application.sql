UPDATE application a
INNER JOIN
  (SELECT a.id,
          coalesce(b.totalGames, 0) totalGames,
          coalesce(b.quizGames, 0) quizGames,
          coalesce(b.moneyGames, 0) moneyGames,
          coalesce(b.bettingGames, 0) bettingGames
   FROM application a
   LEFT JOIN
     (SELECT a.applicationId,
             COUNT(b.id) totalGames,
             COUNT(b.id) quizGames, /* TOD: clarify this */
             SUM(IF(b.isMoneyGame = 1, 1, 0)) moneyGames,
             0 bettingGames /* betting games not available at the moment */
      FROM application_has_game a
      INNER JOIN game b ON a.gameId = b.id
      GROUP BY a.applicationId) b ON a.id = b.applicationId
   WHERE a.id IN (:ids) ) b ON a.id = b.id
SET a.stat_games = b.totalGames,
    a.stat_quizGames = b.quizGames,
    a.stat_moneyGames = b.moneyGames,
    a.stat_bettingGames = b.bettingGames;