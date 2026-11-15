/**
 * API Server for mongsterView
 * 
 * This server acts as a bridge between monsgterView React app and mongsterServer.
 * It exposes REST endpoints that mongsterView expects:
 * - GET /api/messages - Get all messages from MongoDB
 * - POST /api/clear - Clear stored messages via mongsterServer.clear()
 */

import express from 'express';
import mongoose from 'mongoose';
import { MailSchema } from './models/mail.js';

const app = express();
const PORT = process.env.PORT || 3001;
const MONGO_URL = process.env.MONGO_URL || 'mongodb://localhost:27017/mongster';

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// MongoDB connection and model setup
let mongoConnection;
let MailModel;

async function connectDatabase() {
  try {
    mongoConnection = await mongoose.connect(MONGO_URL);
    console.log('MongoDB connected to:', MONGO_URL);
    
    const mailSchema = new mongoose.Schema({
      date: { type: String, required: true },
      subject: { type: String, required: false },
      to: [{ type: String, required: true }],
      from: { type: String, required: true },
      cc: [{ type: String, required: false }],
      replyto: { type: String, required: false },
      headers: [{ name: String, values: [{ value: String }] }],
      body: { type: String }
    }, { collection: 'messages' });
    
    // Register model if it doesn't exist
    MailModel = mongoose.model('Message', mailSchema);
  } catch (error) {
    console.error('Failed to connect to MongoDB:', error);
    throw error;
  }
}

// API Endpoints for mongsterView

/**
 * GET /api/messages - Get all messages from database
 */
app.get('/api/messages', async (req, res) => {
  try {
    await connectDatabase();
    const messages = await MailModel.find({});
    
    // Convert MongoDB objects to plain objects for JSON serialization
    const messagesArray = Array.isArray(messages) ? messages : [];
    const plainMessages = messagesArray.map(msg => ({
      _id: msg._id?.toString(),
      date: msg.date,
      subject: msg.subject,
      to: msg.to,
      from: msg.from,
      cc: msg.cc,
      replyto: msg.replyto,
      headers: msg.headers,
      body: msg.body
    }));
    
    console.log('Returning', plainMessages.length, 'messages');
    res.json({ messages: plainMessages });
  } catch (error) {
    console.error('Error fetching messages:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * POST /api/clear - Clear stored messages via mongsterServer
 */
app.post('/api/clear', async (req, res) => {
  try {
    await connectDatabase();
    
    // Find the Mongster Java class if available
    let Mongster = null;
    try {
      const java = await import('java');
      const jarPath = process.env.MONGSTER_JAR || 'mongster-0.1-SNAPSHOT-jar-with-dependencies.jar';
      
      // Try to load the Java Mongster class via bridge if java package is available
      if (typeof require !== 'undefined' && typeof java === 'function') {
        const Java = require('java');
        Java.classpath.push(process.env.MONGSTER_JAR || jarPath);
        Mongster = Java.import('org.mongster.Mongster');
      }
    } catch (e) {
      console.warn('Java bridge not available, skipping clear operation:', e.message);
    }
    
    if (Mongster) {
      try {
        const config = new Mongster();
        config.setMongoHostSync(new URL(MONGO_URL).hostname);
        config.setMongoPortSync(parseInt(new URL(MONGO_URL).port || 27017));
        config.setMongoDatabaseSync('mongster');
        
        const mongsterInstance = new Mongster(config);
        await mongsterInstance.clear();
      } catch (clearError) {
        console.warn('Mongster clear operation failed:', clearError.message);
      }
    } else {
      // Clear the database directly as fallback
      try {
        await connectDatabase();
        await MailModel.deleteMany({});
        console.log('Cleared all messages from database');
      } catch (dbClearError) {
        console.error('Failed to clear database:', dbClearError);
      }
    }
    
    res.sendStatus(200);
  } catch (error) {
    console.error('Error clearing messages:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * Root route for health check
 */
app.get('/', (req, res) => {
  res.json({ 
    name: 'monsgterView API',
    version: '0.1.0',
    endpoints: [
      { path: '/api/messages', method: 'GET', description: 'Get all messages' },
      { path: '/api/clear', method: 'POST', description: 'Clear stored messages' }
    ]
  });
});

// Start server
const server = app.listen(PORT, () => {
  console.log(`monsgterView API server running on port ${PORT}`);
  console.log('Available endpoints:');
  console.log('  GET  /api/messages - Get all messages');
  console.log('  POST /api/clear    - Clear stored messages');
});

// Graceful shutdown
process.on('SIGINT', async () => {
  try {
    await mongoose.connection.close();
    server.close(() => {
      console.log('Server stopped');
      process.exit(0);
    });
  } catch (error) {
    console.error('Error during shutdown:', error);
    process.exit(1);
  }
});

process.on('SIGTERM', async () => {
  try {
    await mongoose.connection.close();
    server.close(() => {
      console.log('Server stopped');
      process.exit(0);
    });
  } catch (error) {
    console.error('Error during shutdown:', error);
    process.exit(1);
  }
});

export { app, server };
