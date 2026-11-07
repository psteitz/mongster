const { GenericContainer, Wait } = require('testcontainers');

/**
 * Provides a MongoDB server for tests.  If MONGO_URL is set (a connection string
 * without a database name, e.g. mongodb://localhost:27017), that server is used;
 * otherwise a throwaway MongoDB container is started with Testcontainers, which
 * requires a Docker environment.
 *
 * @return {url, stop()} where url is a connection string without database name
 */
exports.startMongo = async function() {
  if (process.env.MONGO_URL) {
    return { url: process.env.MONGO_URL.replace(/\/+$/, ''), stop: async function() {} };
  }
  const container = await new GenericContainer('mongo:7.0')
    .withExposedPorts(27017)
    .withWaitStrategy(Wait.forLogMessage('Waiting for connections'))
    .start();
  return {
    url: 'mongodb://' + container.getHost() + ':' + container.getMappedPort(27017),
    stop: async function() { await container.stop(); }
  };
};
