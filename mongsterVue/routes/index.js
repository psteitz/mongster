/**
 * Loads messages array with all messages and renders 'index'
 * with messages in scope.
 */
exports.index = function(Mail) {
  return function(req, res) {
    Mail.find({}, function(error, messages) {
      res.render('index', {
        title: 'mongsterVue',
        messages : messages
      });
    });
  };
};

/**
 * Dumps all messages to response stream as json.
 */
exports.get = function(Mail) {
  return function(req, res) {
    Mail.find({}, function(error, messages) {
      res.json({messages : messages});
    });
  }
};

exports.clear = function(smtpServer) {
  return function(req, res) {
    smtpServer.clear();
    res.send(200);
  }
}
