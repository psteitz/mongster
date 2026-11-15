# Mongster - MongoDB-backed SMTP Mail Server

A mail server that receives messages via SMTP and stores them in MongoDB.

## Features

- Receives emails via SMTP on configurable port (**default: 2567**)
- Stores messages in MongoDB collection
- Provides API to retrieve, clear, truncate, and tail messages  
- Uses jakarta.mail for Java 17 compatibility
- Base64-encoded message bytes stored in `_id` field

## Prerequisites

- Java 17 or later
- Maven 3.6+
- MongoDB (for storing messages)
- Docker (optional, for running test containers)

## Building

### Development Build

```bash
cd mongsterServer
mvn clean verify
```

This runs:
- Checkstyle validation
- Unit tests
- JaCoCo code coverage
- PMD static analysis

### Quick Build (skip quality gates)

```bash
mvn clean package -Dcheckstyle.skip=true -Dpmd.skip=true
```

## Running Locally

### Using Test Containers (MongoDB automatically started)

```java
import org.mongster.Mongster;
import org.mongster.MongsterConfig;
import org.testcontainers.containers.GenericContainer;

public class App {
    public static void main(String[] args) throws Exception {
        // Start MongoDB container
        GenericContainer mongoDb = new GenericContainer("mongo:7.0")
            .withExposedPorts(27017);
        mongoDb.start();

        // Create and start Mongster (uses default port 2567)
        Mongster mongster = new Mongster(new MongsterConfig(
            "localhost",             // MongoDB host
            mongoDb.getMappedPort(27017),  // MongoDB port
            "mongster",              // Database name
            "messages"               // Collection name
        ));

        mongster.start();

        // ... use Mongster ...

        // Cleanup
        mongster.stop();
    }
}
```

### Without Test Containers (MongoDB must be running)

```java
import org.mongster.Mongster;
import org.mongster.MongsterConfig;

public class App {
    public static void main(String[] args) throws Exception {
        // Default port 2567 - no root privileges needed!
        Mongster mongster = new Mongster(new MongsterConfig(
            "localhost",   // MongoDB host
            27017,         // MongoDB port  
            "mongster",    // Database name
            "messages"     // Collection name
        ));

        mongster.start();
        // ... use Mongster ...
        mongster.stop();
    }
}
```

To use a different port (e.g., 2500):

```java
new Mongster(new MongsterConfig(
    2500,                    // Custom SMTP port
    "localhost",             // MongoDB host
    27017,                   // MongoDB port
    "mongster",              // Database name
    "messages"               // Collection name
));
```

## API Usage

```java
import org.mongster.Mongster;
import org.bson.Document;
import java.util.List;

// Create instance (uses default config with port 2567)
Mongster mongster = new Mongster();

// Or create with custom port
Mongster mongster = new Mongster(new MongsterConfig(
    2500,                    // Custom SMTP port
    "localhost",             // MongoDB host
    27017,                   // MongoDB port
    "mongster",              // Database name
    "messages"               // Collection name
));

// Start the server
mongster.start();

// Check number of messages
int count = mongster.size();

// Get all messages as BSON documents
List<Document> messages = mongster.getMessages();

// Clear all messages
mongster.clear();

// Truncate to keep only first N messages
mongster.truncate(10);

// Keep only last N messages  
mongster.tail(5);

// Stop the server
mongster.stop();
```

## Building for Distribution

Create an executable JAR with dependencies:

```bash
mvn clean package
ls target/mongster-0.1-SNAPSHOT.jar
```

This creates `target/mongster-0.1-SNAPSHOT.jar` with all dependencies included.

## Running from JAR

To run the application from the packaged JAR:

```bash
java -jar target/mongster-0.1-SNAPSHOT.jar \
  --smtp-port 2567 \
  --mongo-host localhost \
  --mongo-port 27017 \
  --mongo-database mongster \
  --mongo-collection messages
```

## Architecture

- **SMTP Server**: Uses David Moten's maintained fork (com.github.davidmoten:subethasmtp)
- **Mail API**: jakarta.mail for Java 17 compatibility  
- **Storage**: MongoDB via mongodb-driver-sync 5.x
- **Message Storage**: Base64-encoded message bytes in `_id` field

## Code Quality Tools

All quality checks are enforced during `mvn verify`:

### Checkstyle (Strict Mode)
- Validates Java code style and conventions
- Configuration: `checkstyle.xml` with suppressions
- Report: `target/checkstyle-result.xml`
- **Fails on violations**

### PMD (Static Analysis)
- Detects potential bugs and code smells
- Configuration: `pmd-rules.xml` (custom permissive ruleset)
- Report: `target/pmd.xml` or generate with `mvn pmd:pmd`
- **Fails on violations**

### JaCoCo (Code Coverage)
- Measures test coverage automatically during tests
- Report location: `target/site/jacoco/index.html`
- View in browser after running `mvn verify`

## Dependencies

```xml
<!-- Main dependencies -->
<dependency>
  <groupId>com.github.davidmoten</groupId>
  <artifactId>subethasmtp</artifactId>
  <version>7.2.2</version>
</dependency>

<dependency>
  <groupId>jakarta.mail</groupId>
  <artifactId>jakarta.mail-api</artifactId>
  <version>2.1.5</version>
</dependency>

<dependency>
  <groupId>org.mongodb</groupId>
  <artifactId>mongodb-driver-sync</artifactId>
  <version>5.14.0</version>
</dependency>

<dependency>
  <groupId>org.eclipse.angus</groupId>
  <artifactId>angus-mail</artifactId>
  <version>2.0.5</version>
</dependency>
```

## Default SMTP Port (Best Practice)

Mongster defaults to port **2567** instead of the traditional port 25 for these reasons:

1. **No root privileges required** - Ports >= 1024 don't require elevated permissions on Linux/macOS
2. **Avoids conflicts** - Doesn't compete with system mail services (Postfix, Sendmail, etc.)
3. **Development-friendly** - Can run alongside other applications without port conflicts

To use a different port, pass it to the constructor:

```java
new Mongster(new MongsterConfig(2500, ...))
```

If you specifically need port 25 (production SMTP server), ensure no other service is using it.

## Development Workflow

1. **Run tests** to ensure functionality:
   ```bash
   mvn test
   ```

2. **Verify code quality**:
   ```bash
   mvn verify
   ```

3. **Review reports** (optional):
   - Checkstyle: `target/checkstyle-result.xml`
   - JaCoCo coverage: `target/site/jacoco/index.html`  
   - PMD analysis: `mvn pmd:pmd` then check `target/pmd.xml`

4. **Deploy to CI/CD** with full quality gates enabled

## Troubleshooting

### MongoDB Connection Failed
- Ensure MongoDB is running or testcontainers can start it
- Check network/firewall for MongoDB port access

### SMTP Port Already in Use
- Default port 2567 is unlikely to be in use, but if it is:
  - Change the port: `new Mongster(new MongsterConfig(2501, ...))`
  - Stop any existing SMTP server on that port

### Checkstyle/PMD Failures  
- Review error messages for specific violations
- See generated reports for detailed analysis

## License

Apache License 2.0 - See LICENSE file for details.

## Contributing

Contributions are welcome! Please follow the code style rules and run `mvn verify` before submitting.

## Container Support

Mongster supports Docker for easy deployment:

```bash
# Build image from source
mvn clean package
docker build -t mongster .

# Run with default settings (port 2567)
docker run -d --name mongster -p 2567:2567 mongster

# Or use docker-compose for local development
docker-compose up -d
```

See `DOCKER.md` for detailed containerization documentation.

