function drawFiveCards(poolConfig, ownedCardIds, options = {}) {
  const random = options.random || Math.random;
  const forceNew = options.forceNew === true;
  let drawnCards = [];

  for (let slotNo = 1; slotNo <= 5; slotNo += 1) {
    const rarity = pickRarity(poolConfig, slotNo, random);
    const card = pickCard(poolConfig, rarity, ownedCardIds, drawnCards, slotNo, random);
    drawnCards.push({
      slotNo,
      card,
      isNewCard: !ownedCardIds.has(card.cardId)
    });
  }

  if (forceNew && !drawnCards.some((drawn) => drawn.isNewCard)) {
    drawnCards = replaceOneWithNewCard(poolConfig, ownedCardIds, drawnCards, random);
  }

  return drawnCards;
}

function pickRarity(poolConfig, slotNo, random = Math.random) {
  const rules = poolConfig.indexes.slotRulesBySlot.get(slotNo) || [];
  if (rules.length === 0) {
    throw new Error(`slot ${slotNo} has no probability rules`);
  }
  return weightedPick(rules, (rule) => rule.probabilityBasisPoints, random).rarity;
}

function pickCard(poolConfig, rarity, ownedCardIds, currentPack, slotNo, random = Math.random) {
  const candidates = poolConfig.indexes.cardsByRarity.get(rarity) || [];
  if (candidates.length === 0) {
    throw new Error(`rarity ${rarity} has no online cards`);
  }
  let validCandidates = candidates.filter((card) => canAppearInCurrentPack(card, currentPack, slotNo));
  if (validCandidates.length === 0) {
    validCandidates = candidates;
  }
  const multiplier = slotNo <= 4 ? newCardMultiplier(poolConfig, ownedCardIds, candidates) : 1;
  return weightedPick(validCandidates, (card) => (ownedCardIds.has(card.cardId) ? 1 : multiplier), random);
}

function canAppearInCurrentPack(card, currentPack, slotNo) {
  const alreadyInPack = currentPack.some((drawn) => drawn.card.cardId === card.cardId);
  if (slotNo >= 4 && alreadyInPack) {
    return false;
  }
  const sameCardCount = currentPack.filter((drawn) => drawn.card.cardId === card.cardId).length;
  if (sameCardCount >= 1) {
    const counts = new Map();
    for (const drawn of currentPack) {
      counts.set(drawn.card.cardId, (counts.get(drawn.card.cardId) || 0) + 1);
    }
    const alreadyHasRepeatedGroup = Array.from(counts.values()).some((count) => count >= 2);
    return !alreadyHasRepeatedGroup;
  }
  return true;
}

function newCardMultiplier(poolConfig, ownedCardIds, rarityCards) {
  if (rarityCards.length === 0) {
    return 1;
  }
  const obtained = rarityCards.filter((card) => ownedCardIds.has(card.cardId)).length;
  const rate = Math.floor((obtained * 100) / rarityCards.length);
  const rule = poolConfig.newCardProtectionRules.find((item) => rate >= item.minCollectionRate && rate <= item.maxCollectionRate);
  return rule ? rule.newCardWeightMultiplier : 1;
}

function replaceOneWithNewCard(poolConfig, ownedCardIds, drawnCards, random = Math.random) {
  for (let index = 0; index < Math.min(4, drawnCards.length); index += 1) {
    const origin = drawnCards[index];
    const candidates = (poolConfig.indexes.cardsByRarity.get(origin.card.rarity) || [])
      .filter((card) => !ownedCardIds.has(card.cardId))
      .filter((card) => canAppearInCurrentPack(card, drawnCards.filter((_, itemIndex) => itemIndex !== index), origin.slotNo));
    if (candidates.length > 0) {
      const replaced = drawnCards.slice();
      const card = weightedPick(candidates, () => 1, random);
      replaced[index] = {
        slotNo: origin.slotNo,
        card,
        isNewCard: true
      };
      return replaced;
    }
  }
  return drawnCards;
}

function weightedPick(items, weightFn, random = Math.random) {
  const total = items.reduce((sum, item) => sum + Math.max(0, Number(weightFn(item)) || 0), 0);
  if (total <= 0) {
    throw new Error("weight total must be greater than zero");
  }
  let cursor = random() * total;
  for (const item of items) {
    cursor -= Math.max(0, Number(weightFn(item)) || 0);
    if (cursor < 0) {
      return item;
    }
  }
  return items[items.length - 1];
}

module.exports = {
  drawFiveCards,
  pickRarity,
  pickCard,
  canAppearInCurrentPack,
  newCardMultiplier,
  weightedPick
};
