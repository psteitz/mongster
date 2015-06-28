// Mongo
var Mongoose = require('mongoose');
var MailSchema = require('./mail.js').MailSchema;
var db = Mongoose.createConnection('localhost', 'mongster');
exports.mailModel = db.model('messages', MailSchema);
