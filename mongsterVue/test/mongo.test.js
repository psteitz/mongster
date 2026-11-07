const { describe, it, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const { MongoClient } = require('mongodb');
const request = require('supertest');
const { createApp } = require('../app');
const db = require('../models/db');
const { startMongo } = require('./helpers/mongo');
const { fakeBackend } = require('./helpers/fakes');

// A document as stored by Mongster: header names lower-cased and dash-free.
const stored = {
  received: 'from localhost by mongster',
  date: 'Sat, 7 Nov 2026 12:00:00 -0700',
  from: 'someone@somewhere.com',
  to: 'anyone@anywhere.com',
  subject: 'hello',
  messageid: '<1@somewhere.com>',
  body: 'hello body',
  envelopeSender: 'someone@somewhere.com',
  envelopeReceiver: 'anyone@anywhere.com',
  sequenceNumber: 0
};

describe('mongoose model against MongoDB', () => {
  let mongo, client, mongoose, app;

  before(async () => {
    mongo = await startMongo();
    client = await MongoClient.connect(mongo.url);
    mongoose = db.connect(mongo.url + '/mongster_model_test');
    app = createApp({ Mail: mongoose.mailModel, smtpServer: fakeBackend([]).smtpServer, log: false });
  });

  after(async () => {
    await mongoose.connection.close();
    await client.close();
    await mongo.stop();
  });

  beforeEach(async () => {
    await client.db('mongster_model_test').collection('messages').deleteMany({});
  });

  it('returns no messages from an empty collection', async () => {
    const res = await request(app).get('/messages.json').expect(200);
    assert.deepEqual(res.body, { messages: [] });
  });

  it('reads messages written in Mongster format', async () => {
    await client.db('mongster_model_test').collection('messages').insertOne({ ...stored });
    const res = await request(app).get('/messages.json').expect(200);
    assert.equal(res.body.messages.length, 1);
    const message = res.body.messages[0];
    assert.equal(message.subject, 'hello');
    assert.equal(message.from, 'someone@somewhere.com');
    assert.deepEqual(message.to, ['anyone@anywhere.com']);
    assert.equal(message.body, 'hello body');
    assert.ok(message._id);
  });

  it('renders stored messages in the page', async () => {
    await client.db('mongster_model_test').collection('messages').insertOne({ ...stored });
    const res = await request(app).get('/').expect(200);
    assert.match(res.text, /hello body/);
  });
});
