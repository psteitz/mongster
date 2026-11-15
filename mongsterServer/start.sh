#!/bin/bash

# ============================================================================
# Mongster Startup Script
# ============================================================================
# Usage: ./start.sh [SMTP_PORT] [MONGO_HOST] [MONGO_PORT]
# ============================================================================

set -e

# Default values
SMTP_PORT=${1:-2567}
MONGO_HOST=${2:-localhost}
MONGO_PORT=${3:-27017}
MONGO_DB=${4:-mongster}
MONGO_COLLECTION=${5:-messages}

# Print configuration
echo "Starting Mongster..."
echo "  SMTP Port: $SMTP_PORT"
echo "  MongoDB Host: $MONGO_HOST:$MONGO_PORT"
echo "  Database: $MONGO_DB"
echo "  Collection: $MONGO_COLLECTION"
echo ""

# Check if JAR exists
if [ ! -f "target/mongster-0.1-SNAPSHOT.jar" ]; then
    echo "Error: mongster JAR not found at target/mongster-0.1-SNAPSHOT.jar"
    echo "Please build first with: mvn clean package"
    exit 1
fi

# Start MongoDB if it's running locally
if [ "$MONGO_HOST" = "localhost" ]; then
    # Check if mongod is running on default port
    if ! pgrep -x "mongod" > /dev/null && ! nc -z localhost $MONGO_PORT 2>/dev/null; then
        echo "Starting local MongoDB..."
        mongod --dbpath /data/db --port $MONGO_PORT --bind_ip_all || true
    fi
fi

# Start Mongster
java -jar target/mongster-0.1-SNAPSHOT.jar \
    org.mongster.Mongster \
    new Mongster(new MongsterConfig($SMTP_PORT, "$MONGO_HOST", $MONGO_PORT, "$MONGO_DB", "$MONGO_COLLECTION"))

# Note: The above is Java syntax - for actual container use, set env vars
# In container, MongoDB connection uses environment variables from docker-compose
