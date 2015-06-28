var express = require('express');
var path = require('path');
var http = require("http");
var favicon = require('serve-favicon');
var logger = require('morgan');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var routes = require('./routes/index');
var app = express();
var router = express.Router();

var Mail = require('./models/db.js').mailModel;
var java = require("java");
java.classpath.push('./mongster-0.1-SNAPSHOT-jar-with-dependencies.jar');

var Mongster = java.import('org.mongster.Mongster');
var smtpServer = new Mongster();
smtpServer.startSync();

// view engine setup
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');
app.set('port', 3000);

// uncomment after placing your favicon in /public
//app.use(favicon(__dirname + '/public/favicon.ico'));
app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: false }));
app.use(cookieParser());
app.use(express.static(path.join(__dirname, 'public')));

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
  console.error(err.stack);
  res.send(500, 'Sorry, something bad happened.');
});

http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));
});
