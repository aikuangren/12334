const fs = require("node:fs");

const SUPPORTED_SCHEMA_VERSION = 1;
const EXPECTED_SLOT_TOTAL = 10000;
const RARITIES = ["N", "R", "SR", "SSR", "UR", "LR", "XR"];

function loadCardPoolConfig(filePath) {
  const config = JSON.parse(fs.readFileSync(filePath, "utf8"));
  validateCardPoolConfig(config, filePath);
  return buildIndexes(config);
}

function validateCardPoolConfig(config, source = "card pool config") {
  if (config.schemaVersion !== SUPPORTED_SCHEMA_VERSION) {
    throw new Error(`${source} schemaVersion only supports ${SUPPORTED_SCHEMA_VERSION}`);
  }
  if (!config.pool || !config.pool.version || !config.pool.name || config.pool.status !== "ONLINE") {
    throw new Error(`${source} pool version/name/status is invalid`);
  }
  if (!config.quota || !Number.isInteger(config.quota.dailyFreeQuota) || config.quota.dailyFreeQuota < 0) {
    throw new Error(`${source} quota.dailyFreeQuota is invalid`);
  }
  if (!Array.isArray(config.cards) || config.cards.length === 0) {
    throw new Error(`${source} cards cannot be empty`);
  }
  if (!Array.isArray(config.slotRules) || config.slotRules.length === 0) {
    throw new Error(`${source} slotRules cannot be empty`);
  }
  if (!Array.isArray(config.newCardProtectionRules) || config.newCardProtectionRules.length === 0) {
    throw new Error(`${source} newCardProtectionRules cannot be empty`);
  }

  const cardIds = new Set();
  const rarityCounts = Object.fromEntries(RARITIES.map((rarity) => [rarity, 0]));
  for (const card of config.cards) {
    if (!Number.isInteger(card.cardId) || !card.word || !card.rarity || !card.status) {
      throw new Error(`${source} card fields are incomplete`);
    }
    if (cardIds.has(card.cardId)) {
      throw new Error(`${source} duplicate cardId ${card.cardId}`);
    }
    cardIds.add(card.cardId);
    rarityCounts[card.rarity] = (rarityCounts[card.rarity] || 0) + 1;
  }

  if (config.rarityCounts) {
    for (const rarity of RARITIES) {
      const expected = config.rarityCounts[rarity] || 0;
      const actual = rarityCounts[rarity] || 0;
      if (expected !== actual) {
        throw new Error(`${source} rarity count mismatch ${rarity}: expected=${expected}, actual=${actual}`);
      }
    }
  }

  const totalsBySlot = new Map();
  const onlineRarities = new Set(config.cards.filter((card) => card.status === "ONLINE").map((card) => card.rarity));
  for (const rule of config.slotRules) {
    if (!Number.isInteger(rule.slotNo) || rule.slotNo < 1 || rule.slotNo > 5 || !rule.rarity || !Number.isInteger(rule.probabilityBasisPoints) || rule.probabilityBasisPoints < 0) {
      throw new Error(`${source} slot rule is invalid`);
    }
    totalsBySlot.set(rule.slotNo, (totalsBySlot.get(rule.slotNo) || 0) + rule.probabilityBasisPoints);
    if (rule.probabilityBasisPoints > 0 && !onlineRarities.has(rule.rarity)) {
      throw new Error(`${source} slot rule references empty rarity ${rule.rarity}`);
    }
  }
  for (let slotNo = 1; slotNo <= 5; slotNo += 1) {
    if ((totalsBySlot.get(slotNo) || 0) !== EXPECTED_SLOT_TOTAL) {
      throw new Error(`${source} slot ${slotNo} probability total must be ${EXPECTED_SLOT_TOTAL}`);
    }
  }

  for (const rule of config.newCardProtectionRules) {
    if (!Number.isInteger(rule.minCollectionRate) || !Number.isInteger(rule.maxCollectionRate) || typeof rule.newCardWeightMultiplier !== "number") {
      throw new Error(`${source} protection rule fields are invalid`);
    }
    if (rule.minCollectionRate < 0 || rule.maxCollectionRate > 100 || rule.minCollectionRate > rule.maxCollectionRate || rule.newCardWeightMultiplier <= 0) {
      throw new Error(`${source} protection rule range is invalid`);
    }
  }
}

function buildIndexes(config) {
  const onlineCards = config.cards
    .filter((card) => card.status === "ONLINE")
    .sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0) || a.cardId - b.cardId);
  const cardsById = new Map(onlineCards.map((card) => [card.cardId, card]));
  const cardsByRarity = new Map();
  for (const rarity of RARITIES) {
    cardsByRarity.set(rarity, onlineCards.filter((card) => card.rarity === rarity));
  }
  const slotRulesBySlot = new Map();
  for (let slotNo = 1; slotNo <= 5; slotNo += 1) {
    slotRulesBySlot.set(slotNo, config.slotRules.filter((rule) => rule.slotNo === slotNo));
  }
  return {
    ...config,
    indexes: {
      onlineCards,
      cardsById,
      cardsByRarity,
      slotRulesBySlot
    }
  };
}

module.exports = {
  RARITIES,
  loadCardPoolConfig,
  validateCardPoolConfig
};
