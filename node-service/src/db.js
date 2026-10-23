const mysql = require("mysql2/promise");
const config = require("./config");

function createPool() {
  return mysql.createPool({
    host: config.mysql.host,
    port: config.mysql.port,
    database: config.mysql.database,
    user: config.mysql.user,
    password: config.mysql.password,
    connectionLimit: config.mysql.connectionLimit,
    charset: "utf8mb4",
    timezone: "+08:00",
    namedPlaceholders: true
  });
}

module.exports = {
  createPool
};
