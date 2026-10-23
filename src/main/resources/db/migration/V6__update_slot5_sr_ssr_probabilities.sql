-- Sync existing runtime draw slot rules with the latest slot 5 probability plan.
--
-- Only update a pool when all slot 5 rarity rows are still using the previous
-- default values, so partially/manual tuned pools are not left in a mixed state:
-- slot 5: SR 80%, SSR 19%, UR 0.5%, LR 0.3%, XR 0.2%

CREATE TEMPORARY TABLE tmp_slot5_default_probability_pool (
    pool_id BIGINT PRIMARY KEY
);

INSERT INTO tmp_slot5_default_probability_pool (pool_id)
SELECT pool_id
FROM draw_slot_rule
WHERE slot_no = 5
  AND rarity IN ('SR', 'SSR', 'UR', 'LR', 'XR')
GROUP BY pool_id
HAVING SUM(CASE WHEN rarity = 'SR' AND probability_basis_points = 9800 THEN 1 ELSE 0 END) = 1
   AND SUM(CASE WHEN rarity = 'SSR' AND probability_basis_points = 100 THEN 1 ELSE 0 END) = 1
   AND SUM(CASE WHEN rarity = 'UR' AND probability_basis_points = 50 THEN 1 ELSE 0 END) = 1
   AND SUM(CASE WHEN rarity = 'LR' AND probability_basis_points = 30 THEN 1 ELSE 0 END) = 1
   AND SUM(CASE WHEN rarity = 'XR' AND probability_basis_points = 20 THEN 1 ELSE 0 END) = 1;

UPDATE draw_slot_rule
SET probability_basis_points = 8000
WHERE slot_no = 5
  AND rarity = 'SR'
  AND pool_id IN (SELECT pool_id FROM tmp_slot5_default_probability_pool);

UPDATE draw_slot_rule
SET probability_basis_points = 1900
WHERE slot_no = 5
  AND rarity = 'SSR'
  AND pool_id IN (SELECT pool_id FROM tmp_slot5_default_probability_pool);

DROP TEMPORARY TABLE tmp_slot5_default_probability_pool;
