const fastify = require("fastify");
const cors = require("@fastify/cors");
const config = require("./config");
const { loadCardPoolConfig } = require("./cardPool");
const { createPool } = require("./db");
const { CardDrawService } = require("./cardDrawService");
const { registerRoutes } = require("./routes");

async function buildApp(options = {}) {
  const app = fastify({
    logger: options.logger ?? true
  });
  await app.register(cors, {
    origin: config.corsOrigin === "*" ? true : config.corsOrigin
  });

  const poolConfig = options.poolConfig || loadCardPoolConfig(config.cardPoolConfigFile);
  const dbPool = options.dbPool || createPool();
  const cardDrawService = options.cardDrawService || new CardDrawService(dbPool, poolConfig);
  await registerRoutes(app, cardDrawService);

  app.addHook("onClose", async () => {
    if (dbPool && typeof dbPool.end === "function") {
      await dbPool.end();
    }
  });

  return app;
}

module.exports = {
  buildApp
};
