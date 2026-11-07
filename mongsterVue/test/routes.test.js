const { describe, it } = require('node:test');
const assert = require('node:assert/strict');
const request = require('supertest');
const { createApp } = require('../app');
const { sampleMessages, fakeBackend } = require('./helpers/fakes');

function appFor(backend) {
  return createApp({ Mail: backend.Mail, smtpServer: backend.smtpServer, log: false });
}

describe('routes', () => {
  it('GET /messages.json returns all messages', async () => {
    const res = await request(appFor(fakeBackend(sampleMessages()))).get('/messages.json').expect(200);
    assert.match(res.headers['content-type'], /json/);
    assert.deepEqual(res.body, { messages: sampleMessages() });
  });

  it('GET / renders the page with the messages embedded for angular', async () => {
    const res = await request(appFor(fakeBackend(sampleMessages()))).get('/').expect(200);
    assert.match(res.headers['content-type'], /html/);
    assert.match(res.text, /<title>mongsterVue<\/title>/);
    assert.match(res.text, /ng-controller="MessageListController"/);
    assert.match(res.text, /track by message\._id/);
    // JSON is escaped into the ng-init attribute, so it cannot break out of it
    assert.match(res.text, /ng-init="setMessages\(\[\{&quot;_id&quot;:&quot;1&quot;/);
    assert.doesNotMatch(res.text, /subject&quot;:&quot;second <b>/);
  });

  it('POST /clear clears the smtp server', async () => {
    const backend = fakeBackend(sampleMessages());
    await request(appFor(backend)).post('/clear').expect(200);
    assert.equal(backend.state.clears, 1);
  });

  it('serves the browser libraries and app scripts', async () => {
    const app = appFor(fakeBackend([]));
    for (const url of [
      '/javascripts/vendor/angular/angular.js',
      '/javascripts/vendor/angular-ui-bootstrap/ui-bootstrap-tpls.js',
      '/javascripts/vendor/bootstrap/css/bootstrap.css',
      '/javascripts/mongsterVueModule.js',
      '/javascripts/controllers/MessageListController.js',
      '/stylesheets/style.css'
    ]) {
      await request(app).get(url).expect(200);
    }
  });

  it('returns 404 for unknown paths', async () => {
    await request(appFor(fakeBackend([]))).get('/nothing/here').expect(404);
  });

  it('returns 500 when the database fails', async (t) => {
    t.mock.method(console, 'error', () => {});
    const backend = fakeBackend([]);
    backend.Mail.find = async function() { throw new Error('db down'); };
    const app = appFor(backend);
    await request(app).get('/messages.json').expect(500);
    await request(app).get('/').expect(500);
  });
});
