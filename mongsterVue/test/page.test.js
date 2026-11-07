const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { JSDOM, VirtualConsole } = require('jsdom');
const { createApp } = require('../app');
const { sampleMessages, fakeBackend } = require('./helpers/fakes');

async function waitFor(condition, what) {
  for (let i = 0; i < 100; i++) {
    if (condition()) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 50));
  }
  assert.fail('timed out waiting for ' + what);
}

// Loads the real page in jsdom, running angular and the app scripts served by the app.
describe('page in a browser', () => {
  let backend, server, dom;
  const errors = [];

  const rows = () => dom.window.document.querySelectorAll('tbody tr');

  before(async () => {
    backend = fakeBackend(sampleMessages());
    const app = createApp({ Mail: backend.Mail, smtpServer: backend.smtpServer, log: false });
    server = http.createServer(app);
    await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
    const virtualConsole = new VirtualConsole();
    virtualConsole.on('jsdomError', (e) => errors.push(e));
    dom = await JSDOM.fromURL('http://127.0.0.1:' + server.address().port + '/', {
      runScripts: 'dangerously', resources: 'usable', pretendToBeVisual: true, virtualConsole
    });
    await waitFor(() => rows().length > 0, 'messages to render');
  });

  after(async () => {
    dom.window.close();
    await new Promise((resolve) => server.close(resolve));
  });

  it('renders one table row per message', () => {
    assert.equal(rows().length, 2);
    const cells = [...rows()[1].querySelectorAll('td')].map((td) => td.textContent.trim());
    assert.equal(cells[1], 'second <b>');
    assert.equal(cells[2], 'a@x.com b@x.com');
    assert.equal(cells[3], 'c@x.com');
    assert.equal(cells[4], 'other@somewhere.com');
    assert.equal(cells[6], 'body two');
  });

  it('loads without script errors', () => {
    assert.deepEqual(errors.map(String), []);
    assert.ok(dom.window.angular.module('ui.bootstrap'));
  });

  it('empties the table when Clear is clicked', async () => {
    dom.window.document.querySelector('button.btn-primary').click();
    await waitFor(() => rows().length === 0, 'table to empty');
    assert.equal(backend.state.clears, 1);
  });
});
