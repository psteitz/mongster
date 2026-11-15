# monsgterView

React application for Mongster SMTP server - a peer to mongsterVue.

## Overview

This is a React-based replacement for the Angular-based mongsterVue application. It connects to the mongsterServer via REST API endpoints:

- `GET /api/messages` - Retrieve all messages from MongoDB
- `POST /api/clear` - Clear stored messages

## Features

- Lists all messages sent through Mongster SMTP server
- Auto-refreshes every 30 seconds (matching original behavior)
- Displays message details: date, subject, recipients (to/cc), sender (from), reply-to address, and body
- "Clear" button to clear stored messages via mongsterServer's clear endpoint
- Responsive design for mobile and desktop

## Getting Started

### Prerequisites

- Node.js >= 22.22.2

### Installation

```bash
npm install
```

### Development

Run the development server:

```bash
npm run dev
```

The app will be available at `http://localhost:3000`

### Building for Production

```bash
npm run build
```

The built files will be in the `dist` directory.

## API Endpoints

The monsgterView app expects mongsterServer to expose these endpoints:

- `/api/messages` (GET) - Returns array of messages from MongoDB
- `/api/clear` (POST) - Clears stored messages and returns success response

mongsterServer must have a reverse proxy or API layer configured to handle these paths and forward to the appropriate internal routes.

## Project Structure

```
monsgterView/
├── index.html          # HTML entry point
├── vite.config.js      # Vite configuration with React plugin
├── package.json        # Dependencies and scripts
├── README.md           # This file
└── src/
    ├── main.jsx        # React entry point
    ├── App.jsx         # Main App component
    ├── api-provider.jsx  # API client wrapper
    ├── index.css       # Global styles
```

## Testing

Run tests with:

```bash
npm run test
```

Or with coverage:

```bash
npm run test:coverage
```

## License

See LICENSE.txt in the monsgter root directory.
