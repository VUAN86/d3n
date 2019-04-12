UPDATE workorder a
INNER JOIN
  (SELECT a.id,
          COALESCE(b.cnt, 0) questionsInTranslation,
          COALESCE(c.cnt, 0) questionTranslationsCompleted,
          COALESCE(d.cnt, 0) questionTranslationsInProgress,
          COALESCE(e.cnt, 0) translationsPublished,
          COALESCE(f.cnt, 0) numberOfLanguages
   FROM workorder a
   LEFT JOIN
     (SELECT workorderId,
             COUNT(DISTINCT questionId) cnt
      FROM question_translation
      WHERE status IN ('draft',
                       'review',
                       'automatic_translation')
      GROUP BY workorderId) b ON a.id = b.workorderId
   LEFT JOIN
     (SELECT workorderId,
             COUNT(id) cnt
      FROM question_translation
      WHERE status IN ('approved',
                       'inactive',
                       'active',
                       'dirty',
                       'archived')
      GROUP BY workorderId) c ON a.id = c.workorderId
   LEFT JOIN
     (SELECT workorderId,
             COUNT(id) cnt
      FROM question_translation
      WHERE status IN ('draft',
                       'review',
                       'automatic_translation')
      GROUP BY workorderId) d ON a.id = d.workorderId
   LEFT JOIN
     (SELECT workorderId,
             COUNT(id) cnt
      FROM question_translation
      WHERE status IN ('active',
                       'dirty')
      GROUP BY workorderId) e ON a.id = e.workorderId
   LEFT JOIN
     (SELECT workorderId,
             COUNT(DISTINCT languageId) cnt
      FROM question_translation
      GROUP BY workorderId
      UNION SELECT a.workorderId,
                   COUNT(DISTINCT languageId) cnt
      FROM question a
      INNER JOIN question_translation b ON a.id = b.questionId
      GROUP BY workorderId) f ON a.id = f.workorderId
   WHERE a.id IN (:ids)
     OR a.id IN
       (SELECT workorderId
        FROM question
        WHERE id IN
            (SELECT questionId
             FROM question_translation
             WHERE workorderId IN (:ids)))) b ON a.id = b.id
SET a.stat_questionsInTranslation = b.questionsInTranslation,
    a.stat_questionTranslationsCompleted = b.questionTranslationsCompleted,
    a.stat_questionTranslationsInProgress = b.questionTranslationsInProgress,
    a.stat_translationsProgressPercentage = IF(a.itemsRequired > b.questionTranslationsCompleted, b.questionTranslationsCompleted / (a.itemsRequired - b.questionTranslationsCompleted), 0),
    a.stat_translationsPublished = b.translationsPublished,
    a.stat_numberOfLanguages = b.numberOfLanguages;