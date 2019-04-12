UPDATE pool a
INNER JOIN
  (SELECT a.id,
          COALESCE(b.cnt, 0) numberOfLanguages
   FROM pool a
   LEFT JOIN
     (SELECT poolId,
             count(DISTINCT languageId) cnt
      FROM
        (SELECT a.poolId,
                b.languageId
         FROM pool_has_question a
         INNER JOIN question_translation b ON a.questionId = b.questionId) a
      GROUP BY poolId) b ON a.id = b.poolId
   WHERE a.id IN
       (SELECT poolId
        FROM pool_has_question
        WHERE questionId IN
            (SELECT questionId
             FROM question_translation
             WHERE ID IN (:ids))) ) b ON a.id = b.id
SET a.stat_numberOfLanguages = b.numberOfLanguages;