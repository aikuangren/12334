const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const { loadCardPoolConfig } = require("../src/cardPool");

const resources = path.resolve(__dirname, "../../src/main/resources");

test("ocean release contains 120 cards with complete, distinct image and audio resources", () => {
  const config = loadCardPoolConfig(path.join(resources, "card-pool/v3.json"));
  const local = JSON.parse(fs.readFileSync(path.join(resources, "static/assets/h5/config/card-pool-v3.json"), "utf8"));
  const { indexes, ...server } = config;
  assert.deepEqual(server, local);
  assert.equal(indexes.onlineCards.length, 120);
  assert.deepEqual(config.rarityCounts, { R: 48, SR: 18, SSR: 18, UR: 12, LR: 12, XR: 12 });
  assert.equal(new Set(config.cards.map(card => card.imageUrl)).size, 120);
  assert.equal(new Set(config.cards.map(card => card.audioUrl)).size, 120);
  for (const card of config.cards) {
    assert.ok(card.meaning && card.arabicMeaning, `Missing translation: ${card.word}`);
    for (const url of [card.imageUrl, card.audioUrl]) {
      assert.ok(fs.statSync(path.join(resources, "static", url)).size > 0, url);
    }
  }
  for (const part of ["", "-top", "-bottom"]) {
    assert.ok(fs.statSync(path.join(resources, `static/assets/h5/pack/ocean-pack${part}.png`)).size > 0);
  }
  assert.deepEqual(config.cards.filter(card => card.word === "manta ray").map(card => card.rarity), ["SSR", "UR"]);
});
