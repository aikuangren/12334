DELETE FROM new_card_protection_rule
WHERE (min_collection_rate = 86 AND max_collection_rate = 95)
   OR (min_collection_rate = 96 AND max_collection_rate = 100);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 0, 30, 2.2
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 0 AND max_collection_rate = 30
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 31, 50, 2.0
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 31 AND max_collection_rate = 50
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 51, 70, 1.75
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 51 AND max_collection_rate = 70
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 71, 85, 1.55
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 71 AND max_collection_rate = 85
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 86, 92, 1.35
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 86 AND max_collection_rate = 92
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 93, 97, 1.2
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 93 AND max_collection_rate = 97
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 98, 99, 1.1
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 98 AND max_collection_rate = 99
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 100, 100, 1.0
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 100 AND max_collection_rate = 100
);

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 2.2
WHERE min_collection_rate = 0 AND max_collection_rate = 30;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 2.0
WHERE min_collection_rate = 31 AND max_collection_rate = 50;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.75
WHERE min_collection_rate = 51 AND max_collection_rate = 70;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.55
WHERE min_collection_rate = 71 AND max_collection_rate = 85;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.35
WHERE min_collection_rate = 86 AND max_collection_rate = 92;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.2
WHERE min_collection_rate = 93 AND max_collection_rate = 97;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.1
WHERE min_collection_rate = 98 AND max_collection_rate = 99;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.0
WHERE min_collection_rate = 100 AND max_collection_rate = 100;
