-- Normalize runtime new-card protection rules to the current production plan.
--
-- This table stores the global draw strategy, not user data. Rebuild it so every
-- environment uses the same release rules and cannot keep unknown overlapping
-- historical/manual intervals.

DELETE FROM new_card_protection_rule;

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
VALUES
    (0, 30, 2.6),
    (31, 50, 2.4),
    (51, 70, 2.2),
    (71, 85, 2.0),
    (86, 95, 1.7),
    (96, 99, 1.4),
    (100, 100, 1.0);

CREATE TEMPORARY TABLE tmp_new_card_protection_rate (
    collection_rate INT PRIMARY KEY
);

INSERT INTO tmp_new_card_protection_rate (collection_rate)
VALUES
    (0), (1), (2), (3), (4), (5), (6), (7), (8), (9),
    (10), (11), (12), (13), (14), (15), (16), (17), (18), (19),
    (20), (21), (22), (23), (24), (25), (26), (27), (28), (29),
    (30), (31), (32), (33), (34), (35), (36), (37), (38), (39),
    (40), (41), (42), (43), (44), (45), (46), (47), (48), (49),
    (50), (51), (52), (53), (54), (55), (56), (57), (58), (59),
    (60), (61), (62), (63), (64), (65), (66), (67), (68), (69),
    (70), (71), (72), (73), (74), (75), (76), (77), (78), (79),
    (80), (81), (82), (83), (84), (85), (86), (87), (88), (89),
    (90), (91), (92), (93), (94), (95), (96), (97), (98), (99),
    (100);

CREATE TEMPORARY TABLE tmp_new_card_protection_validation (
    invalid_count INT NOT NULL CHECK (invalid_count = 0)
);

INSERT INTO tmp_new_card_protection_validation (invalid_count)
SELECT COUNT(*)
FROM (
    SELECT rate.collection_rate, COUNT(rule.id) AS hit_count
    FROM tmp_new_card_protection_rate rate
    LEFT JOIN new_card_protection_rule rule
      ON rate.collection_rate BETWEEN rule.min_collection_rate AND rule.max_collection_rate
    GROUP BY rate.collection_rate
) coverage
WHERE coverage.hit_count <> 1;

DROP TEMPORARY TABLE tmp_new_card_protection_validation;
DROP TEMPORARY TABLE tmp_new_card_protection_rate;
