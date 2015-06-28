//Mail schema
var Mongoose = require('mongoose');
var schema = new Mongoose.Schema({
  date : { type : String, required : true },
  subject: { type : String, required : false },
  to : [{ type : String, required : true }],
  from : { type : String, required : true},
  cc : [{ type : String, required : false}],
  replyto : { type : String, required : false},
  headers : [{ name : String, values : [{ value : String}]}],
  body : { type : String}
}, { collection: 'messages' });

exports.MailSchema = schema;
