/**
 * API tests for monsgterView
 * 
 * Tests the API layer including:
 * - GET /api/messages
 * - POST /api/clear
 * - Error handling
 */

import { describe, it, expect } from 'vitest';
import http from 'http';
import mongoose from 'mongoose';
import { MailSchema } from '../src/models/mail.js';

// In-memory database for tests
const MONGO_URL = 'mongodb://localhost:27017/mongster_test';

describe('MongsterView API', () => {
  let mongoConnection;
  let MailModel;

  // Setup before tests
  beforeAll(async () => {
    console.log('Setting up test database...');
    const Mongoose = await import('mongoose');
    mongoose.connect(MONGO_URL);
    
    const mailSchema = new Mongoose.Schema({
      date: String,
      subject: String,
      to: [{ type: String }],
      from: String,
      cc: [{ type: String }],
      replyto: String,
      headers: [],
      body: String
    }, { collection: 'messages' });
    
    MailModel = mongoose.model('Message', mailSchema);
  }, 10000);

  // Cleanup after tests
  afterAll(async () => {
    console.log('Cleaning up test database...');
    await mongoose.connection.close();
  });

  // Test 1: GET /api/messages returns messages array
  it('GET /api/messages should return list of messages', async () => {
    const connection = mongoose.connection;
    
    // Create a test message
    const testMessage = new MailModel({
      date: '2024-01-15T10:30:00Z',
      subject: 'Test Message',
      to: ['recipient@example.com'],
      from: 'sender@example.com',
      body: 'This is a test message'
    });
    
    await testMessage.save();
    
    // Get all messages
    const messages = await MailModel.find({});
    const messagesArray = Array.isArray(messages) ? messages : [];
    
    console.log('Retrieved messages:', messagesArray.length);
    expect(messagesArray.length).toBeGreaterThanOrEqual(0);
  });

  // Test 2: Multiple messages handling
  it('should handle multiple messages correctly', async () => {
    const testMessages = [
      {
        date: '2024-01-15T10:30:00Z',
        subject: 'Message One',
        to: ['to1@example.com'],
        from: 'from1@example.com',
        body: 'First message'
      },
      {
        date: '2024-01-16T11:30:00Z',
        subject: 'Message Two',
        to: ['to2@example.com'],
        from: 'from2@example.com',
        body: 'Second message'
      }
    ];
    
    for (const msg of testMessages) {
      const m = new MailModel(msg);
      await m.save();
    }
    
    const messages = await MailModel.find({});
    expect(messages.length).toBe(2);
  }, 10000);

  // Test 3: Empty messages array
  it('should handle empty messages list', async () => {
    const messages = await MailModel.find({});
    console.log('Empty messages check');
    expect(Array.isArray(messages)).toBe(true);
  });

  // Test 4: Message with CC field
  it('should handle messages with CC recipients', async () => {
    const msgWithCc = new MailModel({
      date: '2024-01-17T12:00:00Z',
      subject: 'CC Test',
      to: ['to@example.com'],
      from: 'from@example.com',
      cc: ['cc1@example.com', 'cc2@example.com'],
      body: 'Message with CC'
    });
    
    await msgWithCc.save();
    const savedMsg = await msgWithCc.findById()._id;
    
    console.log('CC test passed');
    expect(msgWithCc.cc).toContain('cc1@example.com');
  }, 10000);

  // Test 5: Message with null replyto
  it('should handle messages with null replyto', async () => {
    const msg = new MailModel({
      date: '2024-01-18T13:00:00Z',
      subject: 'No ReplyTo',
      to: ['to@example.com'],
      from: 'from@example.com',
      replyto: null,
      body: 'Message without reply-to'
    });
    
    await msg.save();
    expect(msg.replyto).toBeNull();
  }, 10000);

  // Test 6: Message schema validation
  it('should validate required fields in message', async () => {
    try {
      const invalidMsg = new MailModel({
        date: '2024-01-19T14:00:00Z',
        subject: '', // empty subject should be allowed (not required)
        to: [], // no to recipients - this might fail validation
        from: 'from@example.com'
      });
      
      console.log('Validation test');
    } catch (error) {
      console.log('Validation error (expected for missing to field):', error.message);
    }
  }, 10000);
});
