DELETE FROM new_card_protection_rule
WHERE (min_collection_rate = 51 AND max_collection_rate = 70)
   OR (min_collection_rate = 71 AND max_collection_rate = 85)
   OR (min_collection_rate = 86 AND max_collection_rate = 92)
   OR (min_collection_rate = 93 AND max_collection_rate = 97)
   OR (min_collection_rate = 98 AND max_collection_rate = 99);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 51, 70, 1.8
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 51 AND max_collection_rate = 70
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 71, 85, 1.6
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 71 AND max_collection_rate = 85
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 86, 95, 1.35
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 86 AND max_collection_rate = 95
);

INSERT INTO new_card_protection_rule (min_collection_rate, max_collection_rate, new_card_weight_multiplier)
SELECT 96, 99, 1.15
WHERE NOT EXISTS (
    SELECT 1 FROM new_card_protection_rule
    WHERE min_collection_rate = 96 AND max_collection_rate = 99
);

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.8
WHERE min_collection_rate = 51 AND max_collection_rate = 70;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.6
WHERE min_collection_rate = 71 AND max_collection_rate = 85;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.35
WHERE min_collection_rate = 86 AND max_collection_rate = 95;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.15
WHERE min_collection_rate = 96 AND max_collection_rate = 99;
