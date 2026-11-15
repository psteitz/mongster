# MongsterView API Documentation

## Overview

The monsgterView application exposes a REST API that connects to the mongsterServer to manage SMTP messages. This API is consumed by the React frontend.

## Base URL

- Development: `http://localhost:3001`
- Production: Your configured server address

## Endpoints

### GET /api/messages

Retrieves all stored messages from the MongoDB database.

**Request:**
```
GET /api/messages
Content-Type: application/json
```

**Response:**
```json
{
  "messages": [
    {
      "_id": "507f1f77bcf86cd799439011",
      "date": "2024-01-15T10:30:00Z",
      "subject": "Test Subject",
      "to": ["recipient@example.com"],
      "cc": [],
      "from": "sender@example.com",
      "replyto": null,
      "headers": [],
      "body": "This is a test message body content"
    }
  ]
}
```

**Status Codes:**
- `200 OK` - Successful retrieval
- `500 Internal Server Error` - Database connection failed or query error

---

### POST /api/clear

Clears all stored messages from the MongoDB database. Can optionally invoke mongsterServer's clear() method if Java bridge is available.

**Request:**
```
POST /api/clear
Content-Type: application/json
```

**Response:**
```json
{
  "status": "success"
}
```
Or on success (for Express 4):
```
200 OK
```

**Status Codes:**
- `200 OK` - Messages cleared successfully
- `500 Internal Server Error` - Failed to clear messages

---

## Schema

### Message Document

Each message follows this Mongoose schema:

```javascript
{
  date: { type: String, required: true },        // ISO date string
  subject: { type: String, required: false },   // Optional subject line
  to: [String],                                  // Array of recipient emails
  from: String,                                  // Sender email
  cc: [String],                                  // CC recipients array
  replyto: String || null,                       // Reply-to address or null
  headers: [{ name: String, values: { value: String }}], // Email headers
  body: String                                   // Message body text
}
```

## Error Handling

The API uses standard HTTP status codes and includes error details in responses:

- `400 Bad Request` - Invalid request format
- `401 Unauthorized` - Authentication required (if implemented)
- `403 Forbidden` - Access denied
- `404 Not Found` - Endpoint not found
- `500 Internal Server Error` - Server-side error with message

## CORS

Development mode includes CORS headers to allow cross-origin requests. Production should configure CORS based on your frontend origin.

## Rate Limiting

Not implemented by default. Add as needed for production use.
