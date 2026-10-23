const path = require("node:path");

require("dotenv").config({ path: path.resolve(__dirname, "../.env") });

const rootDir = path.resolve(__dirname, "../..");

function readInt(name, defaultValue) {
  const raw = process.env[name];
  if (raw === undefined || raw === "") {
    return defaultValue;
  }
  const value = Number.parseInt(raw, 10);
  if (Number.isNaN(value)) {
    throw new Error(`${name} must be an integer`);
  }
  return value;
}

function resolveFromService(value) {
  if (path.isAbsolute(value)) {
    return value;
  }
  return path.resolve(__dirname, "..", value);
}

const config = {
  rootDir,
  port: readInt("PORT", 8080),
  corsOrigin: process.env.CORS_ORIGIN || "*",
  cardPoolConfigFile: resolveFromService(process.env.CARD_POOL_CONFIG_FILE || "../src/main/resources/card-pool/v1.json"),
  mysql: {
    host: process.env.MYSQL_HOST || "127.0.0.1",
    port: readInt("MYSQL_PORT", 3306),
    database: process.env.MYSQL_DATABASE || "card_draw",
    user: process.env.MYSQL_USER || "card_draw",
    password: process.env.MYSQL_PASSWORD || "",
    connectionLimit: readInt("MYSQL_CONNECTION_LIMIT", 10)
  }
};

module.exports = config;
