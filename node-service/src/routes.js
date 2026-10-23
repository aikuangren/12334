const { BusinessError } = require("./errors");
const { ok, fail } = require("./response");

async function registerRoutes(app, cardDrawService) {
  app.get("/api/ping", async () => ok(await cardDrawService.health()));

  app.get("/api/card-draw/activity", async (request) => {
    const { userId } = request.query;
    return ok(await cardDrawService.activity(String(userId || "")));
  });

  app.post("/api/card-draw/draw", async (request) => {
    const { userId, requestId } = request.query;
    return ok(await cardDrawService.draw(String(userId || ""), requestId));
  });

  app.get("/api/card-draw/my-cards", async (request) => {
    const { userId } = request.query;
    return ok(await cardDrawService.myCards(String(userId || "")));
  });

  app.get("/api/card-draw/history", async (request) => {
    const { userId } = request.query;
    return ok(await cardDrawService.history(String(userId || "")));
  });

  app.post("/api/card-draw/simulate-next-day", async (request) => {
    const { userId } = request.query;
    return ok(await cardDrawService.simulateNextDay(String(userId || "")));
  });

  app.setErrorHandler((error, request, reply) => {
    if (error instanceof BusinessError) {
      reply.code(error.statusCode).send(fail(error.message));
      return;
    }
    request.log.error(error);
    reply.code(500).send(fail("系统繁忙，请稍后再试"));
  });
}

module.exports = {
  registerRoutes
};
