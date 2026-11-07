var express = require('express');
var path = require('path');
var http = require("http");
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var routes = require('./routes/index');
var db = require('./models/db.js');

// Directories of third-party browser libraries served under /javascripts/vendor
function packageDir(name) {
  return path.dirname(require.resolve(name + '/package.json'));
}

/**
 * Creates the Express app.
 *
 * @param options.Mail mongoose model for stored messages
 * @param options.smtpServer Mongster instance (needs a clear() method)
 * @param options.log set to false to disable request logging
 */
function createApp(options) {
  var Mail = options.Mail;
  var smtpServer = options.smtpServer;
  var app = express();
  var router = express.Router();

  // view engine setup
  app.set('views', path.join(__dirname, 'views'));
  app.set('view engine', 'pug');
  app.set('port', 3000);

  // uncomment after placing your favicon in /public
  //app.use(favicon(__dirname + '/public/favicon.ico'));
  if (options.log !== false) {
    app.use(logger('dev'));
  }
  app.use(bodyParser.json());
  app.use(bodyParser.urlencoded({ extended: false }));
  app.use(cookieParser());
  app.use(express.static(path.join(__dirname, 'public')));
  app.use('/javascripts/vendor/angular', express.static(packageDir('angular')));
  app.use('/javascripts/vendor/angular-ui-bootstrap',
          express.static(path.join(packageDir('angular-ui-bootstrap'), 'dist')));
  app.use('/javascripts/vendor/bootstrap', express.static(path.join(packageDir('bootstrap'), 'dist')));

  app.use('/', router);
  app.get('/', routes.index(Mail));
  app.get('/messages.json', routes.get(Mail));

  app.post('/clear', routes.clear(smtpServer));

  // catch 404 and forward to error handler
  app.use(function(req, res, next) {
    var err = new Error('Not Found');
    err.status = 404;
    next(err);
  });

  // error handler
  app.use(function(err, req, res, next){
    var status = err.status || 500;
    if (status >= 500) {
      console.error(err.stack);
      return res.status(500).send('Sorry, something bad happened.');
    }
    res.status(status).send(err.message);
  });

  return app;
}

/**
 * Starts a Mongster SMTP server (via the Java bridge) that stores messages
 * in the MongoDB database identified by mongoUrl.
 */
function startMongster(options) {
  var java = require("java");
  java.classpath.push(options.jar || process.env.MONGSTER_JAR ||
                      path.join(__dirname, 'mongster-0.1-SNAPSHOT-jar-with-dependencies.jar'));
  var mongo = new URL(options.mongoUrl);
  var MongsterConfig = java.import('org.mongster.MongsterConfig');
  var Mongster = java.import('org.mongster.Mongster');
  var config = new MongsterConfig();
  config.setMongoHostSync(mongo.hostname);
  if (mongo.port) {
    config.setMongoPortSync(parseInt(mongo.port, 10));
  }
  if (mongo.pathname.length > 1) {
    config.setMongoDatabaseSync(mongo.pathname.substring(1));
  }
  var smtpPort = options.smtpPort || process.env.SMTP_PORT;
  if (smtpPort) {
    config.setSmtpPortSync(parseInt(smtpPort, 10));
  }
  var smtpServer = new Mongster(config);
  smtpServer.startSync();
  return smtpServer;
}

/**
 * Java methods called through the bridge without a callback do not wait for
 * completion, so expose clear() as a promise that resolves when it is done.
 */
function asyncClear(javaServer) {
  return {
    clear: function() {
      return new Promise(function(resolve, reject) {
        javaServer.clear(function(err) { err ? reject(err) : resolve(); });
      });
    }
  };
}

/**
 * Starts the SMTP server, connects to MongoDB and serves the web app.
 *
 * Options (all optional): port (default $PORT or 3000), smtpPort (default $SMTP_PORT or 25),
 * mongoUrl (default $MONGO_URL or mongodb://localhost:27017/mongster), jar
 * (default $MONGSTER_JAR or the standalone jar in this directory), log.
 *
 * @return promise for {app, server, smtpServer, connection, close()}
 */
async function start(options) {
  options = options || {};
  options.mongoUrl = options.mongoUrl || process.env.MONGO_URL || db.DEFAULT_URL;
  var port = options.port === undefined ? (process.env.PORT || 3000) : options.port;
  var smtpServer = startMongster(options);
  var mongo = db.connect(options.mongoUrl);
  var app = createApp({ Mail: mongo.mailModel, smtpServer: asyncClear(smtpServer), log: options.log });
  app.set('port', port);
  var server = http.createServer(app);
  await new Promise(function(resolve) { server.listen(port, resolve); });
  console.log('Express server listening on port ' + server.address().port);
  return {
    app: app,
    server: server,
    smtpServer: smtpServer,
    connection: mongo.connection,
    close: async function() {
      await new Promise(function(resolve) { server.close(resolve); });
      smtpServer.stopSync();
      await mongo.connection.close();
    }
  };
}

exports.createApp = createApp;
exports.start = start;

if (require.main === module) {
  start();
}
