const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const nodemailer = require('nodemailer');
const request = require('supertest');
const { start } = require('../app');
const { startMongo } = require('./helpers/mongo');

const SMTP_PORT = 2525;
const jar = process.env.MONGSTER_JAR ||
  path.join(__dirname, '..', '..', 'mongsterServer', 'target', 'mongster-0.1-SNAPSHOT-jar-with-dependencies.jar');

// Runs the whole stack: the Java Mongster SMTP server (through the java bridge), MongoDB and the web app.
// Needs a standalone Mongster jar: run `mvn compile assembly:single` in ../mongsterServer or set MONGSTER_JAR.
describe('end to end', { skip: fs.existsSync(jar) ? false : 'Mongster jar not found: ' + jar }, () => {
  let mongo, running;

  before(async () => {
    mongo = await startMongo();
    running = await start({ port: 0, smtpPort: SMTP_PORT, mongoUrl: mongo.url + '/mongster_e2e_test', jar: jar, log: false });
  });

  after(async () => {
    await running.close();
    await mongo.stop();
  });

  it('shows mail sent over SMTP and clears it', async () => {
    const transport = nodemailer.createTransport({ host: '127.0.0.1', port: SMTP_PORT, secure: false, ignoreTLS: true });
    await transport.sendMail({
      from: 'someone@somewhere.com', to: 'anyone@anywhere.com', subject: 'hello', text: 'hello body'
    });

    const web = request(running.app);
    const res = await web.get('/messages.json').expect(200);
    assert.equal(res.body.messages.length, 1);
    const message = res.body.messages[0];
    assert.equal(message.subject, 'hello');
    assert.equal(message.from, 'someone@somewhere.com');
    assert.deepEqual(message.to, ['anyone@anywhere.com']);
    assert.match(message.body, /hello body/);

    const page = await web.get('/').expect(200);
    assert.match(page.text, /hello body/);

    await web.post('/clear').expect(200);
    const after = await web.get('/messages.json').expect(200);
    assert.deepEqual(after.body, { messages: [] });
  });
});
