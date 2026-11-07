// Mongo
var Mongoose = require('mongoose');
var MailSchema = require('./mail.js').MailSchema;

var DEFAULT_URL = 'mongodb://localhost:27017/mongster';

/**
 * Opens a connection to the Mongster database.
 *
 * @param uri MongoDB connection string; defaults to $MONGO_URL or DEFAULT_URL
 * @return {connection, mailModel}
 */
exports.connect = function(uri) {
  var connection = Mongoose.createConnection(uri || process.env.MONGO_URL || DEFAULT_URL);
  return { connection: connection, mailModel: connection.model('messages', MailSchema) };
};

exports.DEFAULT_URL = DEFAULT_URL;
