# Migration from MongsterVue to MonsgterView

This document describes how to migrate from the Angular-based mongsterVue application to the new React-based monsgterView application.

## Overview

MonsgterView is a peer application designed as a modern alternative to mongsterVue, built with:
- **React 18** instead of Angular 1.x
- **Vite** build tool for faster development
- **Vitest** test framework for comprehensive testing
- **Mongoose** for MongoDB modeling (shared schema)

## Feature Parity

MonsgterView provides all functionality from mongsterVue:

| Feature | MongsterVue | MonsgterView |
|---------|-------------|---------------|
| View SMTP messages | ✓ | ✓ |
| Auto-refresh (30s) | ✓ | ✓ |
| Clear messages button | ✓ | ✓ |
| Message display (date, subject, to/cc/from, replyto, body) | ✓ | ✓ |
| Mobile responsive design | Partial | Full |
| Test coverage | Limited | Comprehensive |
| Hot module reload | No | Yes |

## Quick Start

### 1. Install Dependencies

```bash
cd /home/psteitz/mongster/monsgterView
npm install
```

### 2. Run Development Server

First, ensure mongsterServer is running and MongoDB is accessible. Then:

```bash
# Terminal 1: Start API server
npm run api

# Terminal 2: Start React frontend
npm run dev
```

The app will be available at `http://localhost:3000`

### 3. Build for Production

```bash
npm run build
```

This creates optimized production files in the `dist/` directory.

## API Configuration

MonsgterView expects these endpoints from mongsterServer:

- **GET /api/messages** - Returns array of all messages
- **POST /api/clear** - Clears stored messages

The API server (`api-server.js`) handles MongoDB connection and calls mongsterServer's clear functionality if the Java bridge is available.

## Docker Deployment

For containerized deployment:

```bash
cd /home/psteitz/mongster/monsgterView
docker-compose up --build
```

This starts:
- API server on port 3001
- MongoDB database  
- Development frontend on port 3000

## Testing

Run the complete test suite:

```bash
npm test
```

Test categories:
- `api.test.js` - API endpoint tests
- `api-client.test.js` - Axios client tests  
- `App.test.jsx` - React component tests
- `e2e.test.js` - End-to-end user scenario tests
- `messages.test.js` - Message display tests
- `rendering.test.js` - HTML rendering and CSS tests

Run with coverage:
```bash
npm run test:coverage
```

## Code Style

MonsgterView uses:
- JSX syntax for React components
- ES modules (ESM) format
- Modern JavaScript (ES2022+)
- ESLint for code quality

See `.eslintrc.js` for linting rules.

## Environment Variables

Available when running monsgterView:

| Variable | Default | Description |
|----------|---------|-------------|
| `MONGO_URL` | `mongodb://localhost:27017/mongster` | MongoDB connection string |
| `NODE_ENV` | `development` | Build mode (dev/production) |
| `PORT` | `3001` | API server port |
| `MONGSTER_JAR` | Auto-detect | Path to mongster jar for clear() |

## Gradual Migration Strategy

1. **Parallel Testing**: Run both applications in parallel to ensure parity
2. **Component-by-Component**: Port features gradually if needed
3. **Shared Data Layer**: Both use same Mongoose schema and MongoDB connection
4. **Gradual Deployment**: Replace frontend in production after testing

## Getting Help

- See [README.md](./README.md) for installation instructions
- See [API.md](./API.md) for API documentation  
- See [docker-compose.yml](./docker-compose.yml) for deployment
- For issues, check mongsterServer documentation for backend configuration

## Support

Both applications can coexist while you migrate:
- Backend mongsterServer unchanged
- MongoDB schema unchanged  
- Only frontend technology changes

For questions or issues, refer to the project README and documentation.
