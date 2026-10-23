const assert = require("node:assert/strict");
const path = require("node:path");
const test = require("node:test");
const { loadCardPoolConfig } = require("../src/cardPool");

test("loads static v1 card pool config", () => {
  const configFile = path.resolve(__dirname, "../../src/main/resources/card-pool/v1.json");
  const poolConfig = loadCardPoolConfig(configFile);

  assert.equal(poolConfig.pool.version, "v1");
  assert.equal(poolConfig.pool.status, "ONLINE");
  assert.equal(poolConfig.indexes.onlineCards.length, 153);
  assert.equal(poolConfig.indexes.cardsByRarity.get("XR").length, 2);

  for (let slotNo = 1; slotNo <= 5; slotNo += 1) {
    const total = poolConfig.indexes.slotRulesBySlot.get(slotNo)
      .reduce((sum, rule) => sum + rule.probabilityBasisPoints, 0);
    assert.equal(total, 10000);
  }
});
