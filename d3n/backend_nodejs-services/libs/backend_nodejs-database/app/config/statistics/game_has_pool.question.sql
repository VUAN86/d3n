UPDATE question a
INNER JOIN
  (SELECT a.id,
          coalesce(b.associatedGames, 0) associatedGames
   FROM question a
   LEFT JOIN
     (SELECT questionId,
             COUNT(DISTINCT gameId) associatedGames
      FROM pool_has_question a
      INNER JOIN game_has_pool b ON a.poolId = b.poolId
      GROUP BY questionId) b ON a.id = b.questionId
   WHERE a.id IN
       (SELECT questionId
        FROM pool_has_question
        WHERE poolId IN (:ids)) ) b ON a.id = b.id
SET a.stat_associatedGames = b.associatedGames;