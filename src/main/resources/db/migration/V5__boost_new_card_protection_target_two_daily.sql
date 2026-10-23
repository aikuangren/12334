UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 2.6
WHERE min_collection_rate = 0 AND max_collection_rate = 30;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 2.4
WHERE min_collection_rate = 31 AND max_collection_rate = 50;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 2.2
WHERE min_collection_rate = 51 AND max_collection_rate = 70;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 2.0
WHERE min_collection_rate = 71 AND max_collection_rate = 85;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.7
WHERE min_collection_rate = 86 AND max_collection_rate = 95;

UPDATE new_card_protection_rule
SET new_card_weight_multiplier = 1.4
WHERE min_collection_rate = 96 AND max_collection_rate = 99;
