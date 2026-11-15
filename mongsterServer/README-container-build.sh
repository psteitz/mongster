#!/bin/bash

# Quick script to build and run Mongster in Docker
# Usage: ./README-container-build.sh [image_name] [build_args...]

set -e

IMAGE_NAME=${1:-mongster}
BUILD_ARGS=("${@:2}")

echo "Building Mongster Docker image..."
docker build \
  --tag "$IMAGE_NAME" \
  --target runtime \
  --build-arg JAR_FILE=target/mongster-0.1-SNAPSHOT.jar \
  . 

if [ $? -eq 0 ]; then
    echo ""
    echo "Docker image built successfully!"
    echo "Image tag: $IMAGE_NAME"
    echo ""
    echo "Run with:"
    echo "  docker run -d --name mongster -p 2567:2567 $IMAGE_NAME"
else
    echo "Build failed!"
fi
