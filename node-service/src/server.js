const config = require("./config");
const { buildApp } = require("./app");

async function main() {
  const app = await buildApp();
  await app.listen({
    port: config.port,
    host: "0.0.0.0"
  });
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
