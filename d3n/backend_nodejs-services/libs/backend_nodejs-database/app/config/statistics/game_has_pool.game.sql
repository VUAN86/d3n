UPDATE game a
INNER JOIN
  (SELECT a.id,
          coalesce(b.poolsCount, 0) poolsCount
   FROM game a
   LEFT JOIN
     (SELECT gameId,
             COUNT(DISTINCT poolId) poolsCount
      FROM game_has_pool
      GROUP BY gameId) b ON a.id = b.gameId
   WHERE a.id IN (:ids) ) b ON a.id = b.id
SET a.stat_poolsCount = b.poolsCount;