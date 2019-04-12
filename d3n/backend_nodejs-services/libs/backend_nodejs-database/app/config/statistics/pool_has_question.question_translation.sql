UPDATE question_translation a
INNER JOIN
  (SELECT a.id,
          COALESCE(b.associatedPools, 0) associatedPools,
          COALESCE(c.associatedGames, 0) associatedGames
   FROM question a
   LEFT JOIN
     (SELECT questionId,
             COUNT(DISTINCT poolId) associatedPools
      FROM pool_has_question
      GROUP BY questionId) b ON a.id = b.questionId
   LEFT JOIN
     (SELECT questionId,
             COUNT(DISTINCT gameId) associatedGames
      FROM pool_has_question a
      INNER JOIN game_has_pool b ON a.poolId = b.poolId
      GROUP BY questionId) c ON a.id = c.questionId
   WHERE a.id IN (:ids) ) b ON a.questionId = b.id
SET a.stat_associatedPools = b.associatedPools,
    a.stat_associatedGames = b.associatedGames;