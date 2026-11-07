const { describe, it, beforeEach, afterEach } = require('node:test');
const strict = require('node:assert/strict');
// Values built inside the jsdom window have that window's prototypes, so compare plain copies
const assert = { deepEqual: (actual, expected) => strict.deepEqual(JSON.parse(JSON.stringify(actual)), expected) };
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');

const root = path.join(__dirname, '..');
const scripts = [
  require.resolve('angular/angular.js'),
  require.resolve('angular-mocks/angular-mocks.js'),
  require.resolve('angular-ui-bootstrap/dist/ui-bootstrap-tpls.js'),
  path.join(root, 'public/javascripts/mongsterVueModule.js'),
  path.join(root, 'public/javascripts/controllers/MessageListController.js')
];

describe('MessageListController', () => {
  let window, $scope, $httpBackend, $interval;

  beforeEach(() => {
    window = new JSDOM('<!doctype html><html><body></body></html>', { runScripts: 'outside-only' }).window;
    scripts.forEach((script) => window.eval(fs.readFileSync(script, 'utf8')));
    const injector = window.angular.injector(['ng', 'ngMock', 'mongsterVueApp']);
    $scope = injector.get('$rootScope').$new();
    $httpBackend = injector.get('$httpBackend');
    $interval = injector.get('$interval');
    $httpBackend.expectGET('/messages.json').respond({ messages: [{ _id: '1', subject: 'first' }] });
    injector.get('$controller')('MessageListController', { $scope: $scope });
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    window.close();
  });

  it('loads messages when created', () => {
    assert.deepEqual($scope.messages, []);
    $httpBackend.flush();
    assert.deepEqual($scope.messages, [{ _id: '1', subject: 'first' }]);
  });

  it('setMessages replaces the list', () => {
    $httpBackend.flush();
    $scope.setMessages([{ _id: '2' }]);
    assert.deepEqual($scope.messages, [{ _id: '2' }]);
  });

  it('refreshes every 30 seconds', () => {
    $httpBackend.flush();
    $httpBackend.expectGET('/messages.json').respond({ messages: [] });
    $interval.flush(29000);
    $httpBackend.verifyNoOutstandingRequest();
    $interval.flush(1000);
    $httpBackend.flush();
    assert.deepEqual($scope.messages, []);
  });

  it('stops refreshing when the scope is destroyed', () => {
    $httpBackend.flush();
    $scope.$destroy();
    $interval.flush(60000);
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('clear posts to the server and reloads the list', () => {
    $httpBackend.flush();
    $httpBackend.expectPOST('/clear').respond(200, 'OK');
    $httpBackend.expectGET('/messages.json').respond({ messages: [] });
    $scope.clear();
    $httpBackend.flush();
    assert.deepEqual($scope.messages, []);
  });
});
