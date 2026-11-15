# Containerization for MonsgterView

This document covers Docker and container-related setup for the MonsgterView React application.

## Project Structure

```
monsgterView/
├── docs/MIGRATION.md
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   └── api-provider.jsx
├── test/
│   ├── setup.js
│   └── *.test.*
├── public/vite.svg
├── Dockerfile          # Production image
├── Dockerfile.dev      # Development image with Vite HMR
├── docker-compose.yml  # Multi-container setup (Mongster + MonsgterView)
├── .dockerignore       # Files to exclude from build context
└── docs/DOCKER.md      # This file
```

## Dockerfile.dev - Development Container

Use `Dockerfile.dev` for development with hot module reload:

```bash
# Build the development image
cd monsgterView
docker build -t monsgter-view-dev -f Dockerfile.dev .

# Run in development mode (with Vite HMR)
docker run --rm -it -p 3000:3000 -p 3001:3001 \
  -e MONGO_URL=mongodb://mongo:27017/mongster \
  -e MONGSTER_JAR=/app/mongster.jar \
  monsgter-view-dev \
  npm run dev
```

### Development Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGO_URL` | `mongodb://mongo:27017/mongster` | Database connection (docker-compose provides `mongo` host) |
| `MONGSTER_JAR` | `/app/mongster.jar` | Path to mongster jar (place it at this path in Docker volume) |

### Volume Mounts

```bash
docker run --rm -it \
  -v $(pwd)/mongsterServer/target:/app/mongster.jar:ro \
  -p 3000:3000 \
  monsgter-view-dev
```

## Dockerfile - Production Image

Use `Dockerfile` for production builds:

```bash
# Build the production image
docker build -t monsgter-view-prod -f Dockerfile .

# Run production (served via nginx or your reverse proxy)
docker run --rm -it -p 3001:3001 \
  -e MONGO_URL=mongodb://localhost:27017/mongster \
  monsgter-view-prod \
  npm run api
```

## docker-compose.yml - Full Setup

Run mongsterServer and MonsgterView together:

```bash
# Build all images
docker-compose build

# Start all services
docker-compose up --build

# Stop all services
docker-compose down
```

### Services

| Service | Port(s) | Description |
|---------|---------|-------------|
| `mongsterServer` | 2567 (SMTP), 3000/8080 (Java) | Java backend |
| `mongodb` | 27017 | MongoDB database |
| `monsgterView-api` | 3001 | Express API server |
| `monsgterView-dev` | 3000 | Vite frontend dev server |

### docker-compose Usage Examples

```bash
# Start all services in development mode
docker-compose -f monsgterView/docker-compose.yml up --build

# Start only the API and MongoDB
docker-compose -f monsgterView/docker-compose.yml up mongsterServer mongodb

# Stop all services
docker-compose -f monsgterView/docker-compose.yml down

# Clean up volumes (removes persistent data)
docker-compose -f monsgterView/docker-compose.yml down -v
```

## Building the Images

### From Source

```bash
cd monsgterView

# Build development image
docker build -t monsgter-view-dev -f Dockerfile.dev .

# Build production image
docker build -t monsgter-view-prod -f Dockerfile .
```

### Using docker-compose

```bash
# Use provided docker-compose.yml with custom images
cd monsgterView
docker-compose build --no-cache
```

## Environment Variables in Containers

When running containers, set these environment variables:

| Variable | Description | Example Value |
|----------|-------------|---------------|
| `MONGO_URL` | MongoDB connection string | `mongodb://mongo:27017/mongster` |
| `SMTP_PORT` | SMTP listening port | `2567` or `25` |
| `PORT` | API server port | `3001` |
| `MONGSTER_JAR` | Path to mongster jar (for Java bridge) | `/app/mongster.jar` |

## Debugging Containers

### View logs

```bash
docker-compose logs -f mongsterView-api
docker-compose logs -f monsgterServer
```

### Execute command in running container

```bash
docker-compose exec monsgterView-api npm run dev
docker-compose exec mongodb mongosh --eval "db.adminCommand('ping')"
```

## Security Considerations

- Don't expose MongoDB port to public networks (only for API server)
- Set `SMTP_PORT=2567` by default (non-privileged port)
- Use environment variables for sensitive configuration
- Mount mongster jar as read-only volume in production

## See Also

- [API.md](./API.md) - REST API documentation
- [MIGRATION.md](./MIGRATION.md) - Migrating from mongsterVue to MonsgterView
- Main project [README.md](../../README.md) - Getting started
