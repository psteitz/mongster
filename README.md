Mongster is an extension of [Wiser](https://github.com/voodoodyne/subethasmtp/tree/master/src/main/java/org/subethamail/wiser),
the "fake" SMTP server used to test applications that send email.
Mongster extends Wiser to persist messages in a MongoDB database.  This allows it to be used with non-Java applications and
also enables unit tests or reports to use MongoDB queries to verify that messages with different headers and / or content have been sent. 
  
There are actually two little projects living here:

* Mongster server - a Java project containing the Wiser extension
* MongsterVue - a MEAN app for managing and viewing contents of a Mongster server

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

As a MEAN app, there is no special build step required for MongsterVue. See info below,
however, on requirements for the node environment to run MongsterVue.

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

### MongsterVue ###
The following instructions are for Ubuntu Linux, though modulo the  [this issue](https://github.com/joeferner/node-java/issues/90#issuecomment-45613235), they should in general work for Mac OSX using brew in place of apt-get.  

1. Install and start MongoDB (see the [MongoDB installation docs](https://www.mongodb.com/docs/manual/installation/))
2. Install Node.js 22.22.2 or later (24 LTS recommended)

   ```
   curl -fsSL https://deb.nodesource.com/setup_24.x | sudo -E bash -
   sudo apt-get install -y nodejs
   ```
3. Install MongsterVue dependencies (from `/mongsterVue` relative to the top-level directory of a clone of this repo).
   The `java` module is built from source and links against whichever JVM `$JAVA_HOME` points to at install time, so
   `$JAVA_HOME` must point to a JDK 17+ (the native bridge is unusable with the Mongster jar otherwise) and `g++`, `make`
   and `python3` are needed.  All dependency versions are pinned exactly in `package.json`; there is no separate bower step.

   ```
   npm install
   ```

4. Install Mongster (from `/mongsterServer`)

   The goal here is just to copy a standalone Mongster jar to `/monsgsterVue.`  That can be from a release or built from the Java sources.  To build from Java sources:
   
   Make sure `$JAVA_HOME` is set to point to a JDK 17+.  If maven is not installed, install it:
   
   ```
   sudo apt-get install maven
   ```
   
   Now build the Mongster standalone jar and copy it to `/mongsterVue`
  
   ```
   mvn clean compile assembly:single
   cp target/*.jar ../mongsterVue
   ```

5. Start MongoVue (from `/mongsterVue`)

   ```
   sudo node app.js
   ```
The `sudo` in the launch command is necessary because MongoVue starts a Mongster server, which by default binds to port 25, which is a privileged port.  [FIXME] (https://github.com/psteitz/mongster/issues/2) to avoid this ugliness.   
   
MongsterVue can be configured with environment variables: `PORT` (web port, default 3000), `SMTP_PORT` (default 25, so a
non-privileged port can be used without `sudo`), `MONGO_URL` (default `mongodb://localhost:27017/mongster`) and `MONGSTER_JAR`
(path to the standalone Mongster jar, default the jar in `/mongsterVue`).  The jar in `/mongsterVue` must be built from the
current sources to work with current MongoDB servers.

If you point a browser at http://localhost:3000 you should see a screen with an empty messages table.  To get some messages to appear, you need to direct some outbound SMTP messages to port 25 on the local host.  To stop the server (sic) kill the process.

#### MongsterVue tests ####
Run `npm test` from `/mongsterVue`.  The tests cover the Express routes, the AngularJS controller, the rendered page (in jsdom)
and the Mongoose model.  Like the server tests, they use Testcontainers to start a MongoDB container, so Docker is required
(or set `MONGO_URL`, e.g. `mongodb://localhost:27017`, to use an existing MongoDB server).  The end-to-end test, which sends mail
through the real Mongster server via the Java bridge, needs a standalone jar: build it with `mvn compile assembly:single` in
`/mongsterServer` (the test is skipped if the jar is not found; set `MONGSTER_JAR` to use a different jar).  It also needs the
same JDK 17+ that `$JAVA_HOME` pointed to when `npm install` built the `java` module; a mismatch causes an
`UnsupportedClassVersionError` when the jar is loaded.

## Collaboration ##

[Pull requests](https://github.com/psteitz/mongster/pulls) and [issues](https://github.com/psteitz/mongster/issues) welcome!

Here are [some easy issues](https://github.com/psteitz/mongster/issues?q=is%3Aopen+is%3Aissue+label%3Aeasy) to talk about / get started with.

And here is [a bigger one](https://github.com/psteitz/mongster/issues/7), which should not be that hard; but will be a fair amount of work.

Patches welcome!

## License ##
Apache Software License, version 2.0

All pull requests must be licensable under this license and are assumed to be granted to the project under terms described in the [Apache Individual Contributor License Agreement] (https://www.apache.org/licenses/icla.txt).
