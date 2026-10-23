const CHINA_OFFSET_MS = 8 * 60 * 60 * 1000;
const DAY_MS = 24 * 60 * 60 * 1000;

function businessDate(resetHour = 5, now = new Date()) {
  const chinaTime = new Date(now.getTime() + CHINA_OFFSET_MS);
  const businessTime = chinaTime.getUTCHours() < resetHour
    ? new Date(chinaTime.getTime() - DAY_MS)
    : chinaTime;
  return businessTime.toISOString().slice(0, 10);
}

module.exports = {
  businessDate
};
