Mongster is an extension of [Wiser](https://github.com/voodoodyne/subethasmtp/tree/master/src/main/java/org/subethamail/wiser),
the "fake" SMTP server used to test applications that send email.
Mongster extends Wiser to persist messages in a MongoDB database.  This allows it to be used with non-Java applications and
also enables unit tests or reports to use MongoDB queries to verify that messages with different headers and / or content have been sent. 
  
There are actually two little projects living here:

* Mongster server - a Java project containing the Wiser extension
* MonsgterView - a React application for managing and viewing contents of a mongsterServer

## Building ##
To build the mongster server, run `mvn clean package` from `/mongsterServer` to get a jar suitable for inclusion
in a Java project or `mvn clean compile assembly:single` to get a standalone jar.  For the build to work, you need to have a JDK
(at least 17) and [maven](http://maven.apache.org) installed.

### Tests ###
The Mongster server unit tests (JUnit 6) use [Testcontainers](https://testcontainers.com) to start a throwaway
MongoDB 7.0 server in a Docker container, so no locally running MongoDB is needed.  A working Docker
(or Docker-compatible) environment is required instead; the first run will pull the `mongo:7.0` image.
Run the tests with `mvn test` from `/mongsterServer`.  Because `mvn package` and `mvn install` also run the tests,
use `-DskipTests` with them if Docker is not available.

For containerization and Docker-related instructions, see [monsgterView/docs/DOCKER.md](monsgterView/docs/DOCKER.md).

As an ESM (ES Modules) app, there is no special build step required for MonsgterView. See the setup info below,
however, on requirements for the node environment to run MonsgterView.

## Running ##
### Mongster Server ###
You can start a Monsgter server from Java code by instantiating a Mongster and invoking
its `start()` method.  Executing `java -jar <path-to-standalone-jar>` from the command line will
also start a Mongster server.  In both cases, a MongoDB server must be running with the 
expected configuration.

The default expected configuration is

| Parameter  | value   |
|------------|---------|
|MongoDB host| localhost|
|MongoDB port | 27017 |
|Mongo database | monsgter |
|Mongo collection | messages |
|SMTP (Wiser) host | localhost |
|SMTP port | 25

Monsgster will try to create the database and collection if they do not already exist on the MongoDB host.  The Mongster
constructor takes an optional `MongsterConfig` argument that allows all of these properties
to be configured.  There is also an `inMemory` config parameter that allows in-memory
storage of messages (as `WiserMessages`) to be turned on / off.  The default is on.

### MonsgterView ###
The following instructions are for Ubuntu Linux, though modulo the  [this issue](https://github.com/joeferner/node-java/issues/90#issuecomment-45613235), they should in general work for Mac OSX using brew in place of apt-get.  

1. Install and start MongoDB (see the [MongoDB installation docs](https://www.mongodb.com/docs/manual/installation/))
2. Install Node.js 22.22.2 or later (24 LTS recommended)

   ```
   curl -fsSL https://deb.nodesource.com/setup_24.x | sudo -E bash -
   sudo apt-get install -y nodejs
   ```
3. Install MonsgterView dependencies (from `/monsgterView` relative to the top-level directory of a clone of this repo).
   The `java` module is built from source and links against whichever JVM `$JAVA_HOME` points to at install time, so
   `$JAVA_HOME` must point to a JDK 17+ (the native bridge is unusable with the Mongster jar otherwise) and `g++`, `make`
   and `python3` are needed.  All dependency versions are pinned exactly in `package.json`; there is no separate bower step.

   ```
   npm install
   ```

4. Install mongsterServer (from `/mongsterServer`)

   The goal here is just to copy a standalone Mongster jar to where it can be accessed.  That can be from a release or built from the Java sources.  To build from Java sources:
   
   Make sure `$JAVA_HOME` is set to point to a JDK 17+.  If maven is not installed, install it:
   
   ```
   sudo apt-get install maven
   ```
   
   Now build the Mongster standalone jar and copy it where needed.
  
   ```
   cd mongsterServer
   mvn clean compile assembly:single
   ```

5. Start MonsgterView (from `/monsgterView`)

   ```
   cd monsgterView
   npm install
   npm run api    # Terminal 1: Start API server
   npm run dev    # Terminal 2: Start React frontend
   ```
The `java` module needs `$JAVA_HOME` to point to a JDK 17+.  All dependency versions are pinned exactly in `package.json`.  

MonsgterView can be configured with environment variables: `PORT` (API server port, default 3001), `SMTP_PORT` (default 2567 for testing without sudo - change to 25 if you want to use the standard SMTP port), `MONGO_URL` (default `mongodb://localhost:27017/mongster`) and `MONGSTER_JAR`
(path to the standalone Mongster jar).

If you point a browser at http://localhost:3000 you should see a screen with an empty messages table.  To get some messages to appear, configure your mail-sending application to send outbound SMTP messages to port **2567** on localhost (the default testing port), or use `SMTP_PORT=25` if mongsterServer is running without privilege restrictions.

#### MonsgterView tests ####
Run `npm test` from `/monsgterView`.  The tests cover the React components, API client, the rendered page (in happy-dom)
and the Mongoose model.  Like the server tests, they use Testcontainers to start a MongoDB container, so Docker is required
(or set `MONGO_URL`, e.g. `mongodb://localhost:27017`, to use an existing MongoDB server).  The end-to-end test, which sends mail
through the real Mongster server via the Java bridge, needs a standalone jar: build it with `mvn compile assembly:single` in
`/mongsterServer` (the test is skipped if the jar is not found; set `MONGSTER_JAR` to use a different jar).  It also needs the
same JDK 17+ that `$JAVA_HOME` pointed to when `npm install` built the `java` module; a mismatch causes an
`UnsupportedClassVersionError` when the jar is loaded.


### MonsgterView ###
MonsgterView is a React application.  

#### Features ####
- Auto-refresh every 30 seconds
- Displays message details: date, subject, to/cc/from fields, replyto, and body
- "Clear" button to clear stored messages
- Mobile-responsive design
- Comprehensive test suite with Vitest

#### Setup ####
To run MonsgterView:

1. Install Node.js 22.22.2 or later (same functionality requirements)

   ```
   curl -fsSL https://deb.nodesource.com/setup_24.x | sudo -E bash -
   sudo apt-get install -y nodejs
   ```

2. Navigate to the MonsgterView directory and install dependencies:

   ```
   cd monsgterView
   npm install
   ```

3. Start the API server (connects to mongsterServer):

   ```
   npm run api
   ```

4. Start the React frontend in development mode:

   ```
   npm run dev
   ```

Alternatively, use Docker for easier deployment.

For full containerization instructions, see [monsgterView/docs/DOCKER.md](monsgterView/docs/DOCKER.md).  Briefly:

   ```
   cd monsgterView
   docker-compose up --build
   ```

The frontend will be available at `http://localhost:3000` and the API server at `http://localhost:3001`.

#### Running Tests ####
To run all tests for MonsgterView:

   ```
   cd monsgterView

   ```
   cd monsgterView
   npm test
   ```

This runs the full test suite including unit tests, integration tests, and E2E tests using Vitest with happy-dom.

To see code coverage:

   ```
   npm run test:coverage
   ```

#### API Endpoints ####
The MonsgterView application connects to mongsterServer and exposes these REST endpoints:

- `GET /api/messages` - Retrieves all messages from MongoDB  
- `POST /api/clear` - Clears stored messages via mongsterServer.clear() method

See [API.md](monsgterView/API.md) for detailed API documentation.

## Collaboration ##

[Pull requests](https://github.com/psteitz/mongster/pulls) and [issues](https://github.com/psteitz/mongster/issues) welcome!

Here are [some easy issues](https://github.com/psteitz/mongster/issues?q=is%3Aopen+is%3Aissue+label%3Aeasy) to talk about / get started with.

And here is [a bigger one](https://github.com/psteitz/mongster/issues/7), which should not be that hard; but will be a fair amount of work.

Patches welcome!

## License ##
Apache Software License, version 2.0

All pull requests must be licensable under this license and are assumed to be granted to the project under terms described in the [Apache Individual Contributor License Agreement] (https://www.apache.org/licenses/icla.txt).
