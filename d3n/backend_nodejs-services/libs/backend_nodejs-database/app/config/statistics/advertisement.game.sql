UPDATE game a
INNER JOIN
  (SELECT a.advertisementProviderId,
          COALESCE(b.adsCount, 0) adsCount
   FROM game a
   LEFT JOIN
     (SELECT advertisementProviderId,
             COUNT(id) adsCount
      FROM advertisement
      GROUP BY advertisementProviderId) b ON a.advertisementProviderId = b.advertisementProviderId
   WHERE a.advertisementProviderId IN (:ids) ) b ON a.advertisementProviderId = b.advertisementProviderId
SET a.stat_adsCount = b.adsCount;