const assert = require("node:assert/strict");
const path = require("node:path");
const test = require("node:test");
const { loadCardPoolConfig } = require("../src/cardPool");
const { drawFiveCards } = require("../src/drawLogic");

function fixedRandom(value) {
  return () => value;
}

test("draws five cards with pack duplicate limits", () => {
  const configFile = path.resolve(__dirname, "../../src/main/resources/card-pool/v1.json");
  const poolConfig = loadCardPoolConfig(configFile);

  const cards = drawFiveCards(poolConfig, new Set(), { random: fixedRandom(0.2) });

  assert.equal(cards.length, 5);
  assert.deepEqual(cards.map((item) => item.slotNo), [1, 2, 3, 4, 5]);

  const cardCounts = new Map();
  for (const item of cards) {
    cardCounts.set(item.card.cardId, (cardCounts.get(item.card.cardId) || 0) + 1);
  }
  assert.ok(Array.from(cardCounts.values()).every((count) => count <= 2));
  assert.notEqual(cards[3].card.cardId, cards[0].card.cardId);
  assert.notEqual(cards[4].card.cardId, cards[0].card.cardId);
});

test("forceNew protection can replace an owned card when a new candidate exists", () => {
  const configFile = path.resolve(__dirname, "../../src/main/resources/card-pool/v1.json");
  const poolConfig = loadCardPoolConfig(configFile);
  const ownedCardIds = new Set(poolConfig.indexes.onlineCards.map((card) => card.cardId));
  const firstNCard = poolConfig.indexes.cardsByRarity.get("N")[0];
  ownedCardIds.delete(firstNCard.cardId);

  const cards = drawFiveCards(poolConfig, ownedCardIds, {
    forceNew: true,
    random: fixedRandom(0.2)
  });

  assert.ok(cards.some((item) => item.isNewCard));
});
