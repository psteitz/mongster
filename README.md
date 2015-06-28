Mongster is an extension of [Wiser](https://github.com/voodoodyne/subethasmtp/tree/master/src/main/java/org/subethamail/wiser),
the "fake" SMTP server used to test applications that send email.
Mongster extends Wiser to persist messages in a MongoDB database.  This allows it to be used with non-Java applications and
also enables unit tests or reports to use MongoDB queries to verify that messages with different headers and / or content have been sent. 
  
There are actually two little projects living here:

* Mongster server - a Java project containing the Wiser extension
* MongsterVue - a MEAN app for managing and viewing contents of a Mongster server

## Building ##
To build the mongster server, run `mvn clean package` to get a jar suitable for inclusion
in a Java project or `mvn clean compile assembly:single` to get a standalone jar.  For the build to work, you need to have a JDK
(at least 1.7) and [maven](http://maven.apache.org) installed.

As a MEAN app, there is no special build step required for MongsterVue. See info below,
however, on requirements for the node environment to run MongsterVue.

## Running ##
###Mongster Server###
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
###MongsterVue###
The following instructions are for Ubuntu Linux, though modulo the  [this issue](https://github.com/joeferner/node-java/issues/90#issuecomment-45613235), they should in general work for Mac OSX using brew in place of apt-get.  

1. Install and start mongo 
   
   ```
   sudo apt-get install mongodb-server
   ```
2. Install node 

   ```
  curl --silent --location https://deb.nodesource.com/setup_0.12 | sudo bash -
   sudo apt-get install --yes nodejs
   ```
3. Install bower

   ```
   sudo npm install bower -g
   ``` 
    
4. Install MongoVue dependencies (from `/mongsterVue` relative to the top-level directory of a clone of this repo)

   ```
   bower install
   npm install -d
   ```
   
5. Install Mongster (from `/mongsterServer`)

   The goal here is just to copy a standalone Mongster jar to `/monsgsterVue.`  That can be from a release or built from the Java sources.  To build from Java sources:
   
   Make sure `$jAVA_HOME` is set to point to a JDK 1.7+.  If maven is not installed, install it:
   
   ```
   sudo apt-get install maven
   ```
   
   Now build the Mongster standalone jar and copy it to `/mongsterVue`
  
   ```
   mvn clean compile assembly:single
   cp target/*.jar ../mongsterVue
   ```

6. Start MongoVue (from `/mongsterVue`)

   ```
   sudo node app.js
   ```
   The `sudo` in the launch command is necessary because MongoVue starts a Mongster server, which by default binds to port 25, which is a privileged port.  (FIXME: expose Mongster port config to MongoVue)
   
   
If you point a browser at http://localhost:3000 you should see a screen with an empty messages table.  To get some messages to appear, you need to direct some outbound SMTP messages to port 25 on the local host.  To stop the server (sic) kill the process.

## Collaboration ##
Discussion happens on the [Mongster Forum](http://ost.io/@psteitz/mongster)

[Pull requests](https://github.com/pulls) and [issues](https://github.com/issues) welcome!

Here are some easy issues to talk about / get started with:


And here is a bigger one, which should not be that hard; but will be a fair amount of work:

## License ##
Apache Software License, version 2.0

All pull requests must be licensable under this license and are assumed to be granted to the project under terms described in the [Apache Individual Contributor License Agreement] (https://www.apache.org/licenses/icla.txt).


 