UPDATE pool a
INNER JOIN
  (SELECT a.id,
          COALESCE(b.c1, 0) questionsComplexityLevel1,
          COALESCE(b.c2, 0) questionsComplexityLevel2,
          COALESCE(b.c3, 0) questionsComplexityLevel3,
          COALESCE(b.c4, 0) questionsComplexityLevel4,
          COALESCE(c.cnt, 0) numberOfLanguages
   FROM pool a
   LEFT JOIN
     (SELECT poolId,
             SUM(c1) c1,
             SUM(c2) c2,
             SUM(c3) c3,
             SUM(c4) c4
      FROM
        (SELECT poolId,
                CASE
                    WHEN complexity = 1 THEN 1
                    ELSE 0
                END AS c1,
                CASE
                    WHEN complexity = 2 THEN 1
                    ELSE 0
                END AS c2,
                CASE
                    WHEN complexity = 3 THEN 1
                    ELSE 0
                END AS c3,
                CASE
                    WHEN complexity = 4 THEN 1
                    ELSE 0
                END AS c4
         FROM question a
         INNER JOIN pool_has_question b ON a.id = b.questionId) q
      GROUP BY poolId) b ON a.id = b.poolId
   LEFT JOIN
     (SELECT poolId,
             count(DISTINCT languageId) cnt
      FROM
        (SELECT a.poolId,
                b.languageId
         FROM pool_has_question a
         INNER JOIN question_translation b ON a.questionId = b.questionId) a
      GROUP BY poolId) c ON a.id = c.poolId
   WHERE a.id IN
       (SELECT poolId
        FROM pool_has_question
        WHERE questionId IN (:ids)) ) b ON a.id = b.id
SET a.stat_questionsComplexityLevel1 = b.questionsComplexityLevel1,
    a.stat_questionsComplexityLevel2 = b.questionsComplexityLevel2,
    a.stat_questionsComplexityLevel3 = b.questionsComplexityLevel3,
    a.stat_questionsComplexityLevel4 = b.questionsComplexityLevel4,
    a.stat_numberOfLanguages = b.numberOfLanguages;