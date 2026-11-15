# Containerizing Mongster

This guide shows how to build and run Mongster in Docker containers.

## Building the Docker Image

### Option 1: Build locally from source

```bash
cd mongsterServer

# Build the JAR first
mvn clean package

# Build Docker image
docker build -t mongster:latest .
```

### Option 2: Build with quality gates (recommended for CI)

The Dockerfile runs `mvn clean package` which includes Checkstyle and PMD checks.

## Running Mongster with Docker

### Basic Run

```bash
docker run -d \
  --name mongster \
  -p 2567:2567 \
  -e MONGODB_HOST=mongodb \
  -e MONGODB_PORT=27017 \
  -e SMTP_PORT=2567 \
  mongster
```

### With MongoDB and Health Checks (Recommended)

```bash
docker-compose up -d
```

This starts both Mongster and MongoDB with proper networking.

### Using Your Own MongoDB Host

```bash
docker run -d \
  --name mongster \
  -p 2567:2567 \
  -e MONGODB_HOST=your-mongodb-hostname \
  -e MONGODB_PORT=27017 \
  mongster
```

## Docker Compose

See `docker-compose.yml` for a full development setup that includes:
- Mongster application container
- MongoDB container (testcontainers)
- Port mapping to non-privileged ports
- Health checks
- Volume persistence for MongoDB data

### Run Development Environment

```bash
docker-compose up -d
```

### View Logs

```bash
docker-compose logs -f mongster
docker-compose logs -f mongodb
```

### Stop Development Environment

```bash
docker-compose down
# Or remove volumes too: docker-compose down -v
```

## Configuration via Environment Variables

The Mongster class uses environment variables for MongoDB connection. Set these before starting:

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGODB_HOST` | localhost | MongoDB hostname or IP |
| `MONGODB_PORT` | 27017 | MongoDB port |
| `MONGODB_DATABASE` | mongster | Database name |
| `MONGODB_COLLECTION` | messages | Collection name |
| `SMTP_PORT` | 2567 | SMTP server port |

Example with custom values:

```bash
docker run -d \
  --env-file .env \
  mongster
```

Create `.env` file:

```
MONGODB_HOST=192.168.1.100
MONGODB_PORT=27017
MONGODB_DATABASE=mongster
MONGODB_COLLECTION=messages
SMTP_PORT=2567
```

## Production Considerations

### Security

The Docker image uses:
- Non-root user (UID 1000) for running the application
- Minimal base image (Alpine Java)
- Health checks for container orchestration

### Networking

- Port 2567 exposed (can be changed via `SMTP_PORT` env var)
- MongoDB connection via environment variable
- Suitable for Kubernetes or Docker Swarm deployment

### Resource Limits (Optional)

Add to your docker-compose.yml or docker run command:

```yaml
deploy:
  resources:
    limits:
      cpus: '1'
      memory: 512M
```

## Troubleshooting

### Container Won't Start

Check if MongoDB connection is working:

```bash
docker exec -it mongster bash
# Inside container, test MongoDB connection
mongosh --host mongodb --eval "db.adminCommand('ping')"
```

### View Application Logs

```bash
docker logs mongster
```

### Port Already in Use

Change SMTP port:

```bash
docker run -d \
  -p 2501:2567 \
  -e SMTP_PORT=2501 \
  mongster
```

## Kubernetes (Optional)

A `kubernetes.yml` manifest could be created for K8s deployment. See documentation for more advanced use cases.

## License

This project and its Docker configuration are licensed under the same terms as the main project.
