/**
 * Loads messages array with all messages and renders 'index'
 * with messages in scope.
 */
exports.index = function(Mail) {
  return async function(req, res) {
    const messages = await Mail.find({});
    res.render('index', {
      title: 'mongsterVue',
      messages : messages
    });
  };
};

/**
 * Dumps all messages to response stream as json.
 */
exports.get = function(Mail) {
  return async function(req, res) {
    const messages = await Mail.find({});
    res.json({messages : messages});
  };
};

exports.clear = function(smtpServer) {
  return async function(req, res) {
    await smtpServer.clear();
    res.sendStatus(200);
  };
};
