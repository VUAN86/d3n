UPDATE pool a
INNER JOIN
  (SELECT a.id,
          COALESCE(b.assignedWorkorders, 0) assignedWorkorders
   FROM pool a
   LEFT JOIN
     (SELECT poolId,
             COUNT(DISTINCT workorderId) assignedWorkorders
      FROM workorder_has_pool
      GROUP BY poolId) b ON a.id = b.poolId
   WHERE a.id IN (:ids) ) b ON a.id = b.id
SET a.stat_assignedWorkorders = b.assignedWorkorders;