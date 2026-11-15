# Dockerfile for monsgterView API Server
# Note: The React frontend should be built separately and served via a reverse proxy

FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Production stage
FROM node:22-alpine

WORKDIR /app

ENV NODE_ENV=production

COPY --from=builder /app/package*.json ./
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/api-server.js ./
COPY --from=builder /app/src/models ./src/models

# Install dependencies for the API server
RUN npm install express mongoose cors

EXPOSE 3001

CMD ["node", "api-server.js"]
