function ok(data) {
  return {
    success: true,
    message: "ok",
    data
  };
}

function fail(message) {
  return {
    success: false,
    message,
    data: null
  };
}

module.exports = {
  ok,
  fail
};
