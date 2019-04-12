UPDATE workorder a
INNER JOIN
  (SELECT a.id,
          COALESCE(b.cnt, 0) questionsCompleted,
          COALESCE(c.cnt, 0) questionsInProgress,
          COALESCE(d.cnt, 0) questionsPublished,
          e.c1 questionsComplexityLevel1,
          e.c2 questionsComplexityLevel2,
          e.c3 questionsComplexityLevel3,
          e.c4 questionsComplexityLevel4,
          COALESCE(f.cnt, 0) numberOfLanguages
   FROM workorder a
   LEFT JOIN
     (SELECT workorderId,
             COUNT(id) cnt
      FROM question
      WHERE status IN ('approved',
                       'inactive',
                       'active',
                       'dirty',
                       'archived')
      GROUP BY workorderId) b ON a.id = b.workorderId
   LEFT JOIN
     (SELECT workorderId,
             COUNT(id) cnt
      FROM question
      WHERE status IN ('draft',
                       'review')
      GROUP BY workorderId) c ON a.id = c.workorderId
   LEFT JOIN
     (SELECT workorderId,
             COUNT(id) cnt
      FROM question
      WHERE status IN ('active',
                       'dirty')
      GROUP BY workorderId) d ON a.id = d.workorderId
   LEFT JOIN
     (SELECT workorderId,
             SUM(c1) c1,
             SUM(c2) c2,
             SUM(c3) c3,
             SUM(c4) c4
      FROM
        (SELECT workorderId,
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
         FROM question) q
      GROUP BY workorderId) e ON a.id = e.workorderId
   LEFT JOIN
     (SELECT workorderId,
             count(DISTINCT languageId) cnt
      FROM
        (SELECT a.workorderId,
                b.languageId
         FROM question a
         INNER JOIN question_translation b ON a.id = b.questionId) a
      GROUP BY workorderId) f ON a.id = f.workorderId
   WHERE a.id IN (:ids)) b ON a.id = b.id
SET a.stat_questionsRequested = a.itemsRequired - b.questionsCompleted,
    a.stat_questionsCompleted = b.questionsCompleted,
    a.stat_questionsInProgress = b.questionsInProgress,
    a.stat_progressPercentage = IF(a.itemsRequired > b.questionsCompleted, b.questionsCompleted / (a.itemsRequired - b.questionsCompleted), 0),
    a.stat_questionsPublished = b.questionsPublished,
    a.stat_questionsComplexityLevel1 = b.questionsComplexityLevel1,
    a.stat_questionsComplexityLevel2 = b.questionsComplexityLevel2,
    a.stat_questionsComplexityLevel3 = b.questionsComplexityLevel3,
    a.stat_questionsComplexityLevel4 = b.questionsComplexityLevel4,
    a.stat_numberOfLanguages = b.numberOfLanguages;