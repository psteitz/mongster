/* 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongster;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;

import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.server.SMTPServer;

/**
 * A mail server using David Moten's SMTP library to receive messages and persist them to MongoDB.
 */
public class Mongster {
    private final int smtpPort;
    private SMTPServer server;
    private MongoCollection<Document> coll;
    
    /**
     * Creates a Mongster with default configuration (localhost:2567, MongoDB at localhost:27017).
     */
    public Mongster() {
        this(new MongsterConfig());
    }
    
    /**
     * Creates a Mongster with the provided configuration.
     * 
     * @param config configuration settings
     */
    public Mongster(MongsterConfig config) {
        this.smtpPort = config.getPort();  // Initialize port from config (defaults to 2567 for testing)
        try {
            String mongoUrl = "mongodb://" + config.getMongoHost() + ":" + config.getMongoPort();
            MongoClient mongoClient = MongoClients.create(mongoUrl);
            coll = mongoClient.getDatabase(config.getMongoDatabase())
                .getCollection(config.getMongoCollection());
        } catch (Exception e) {
            System.err.println("MongoDB connection failed: " + e.getMessage());
            coll = null;
        }
    }
    
    /**
     * Returns the list of messages received since startup or last clear().
     */
    public List<Document> getMessages() {
        if (coll == null) return new ArrayList<>();
        
        // Return all documents from MongoDB collection
        List<Document> result = new ArrayList<>();
        coll.find(new Document()).forEach(result::add);
        
        return result;
    }
    
    /**
     * Starts the SMTP server.
     */
    public void start() throws Exception {
        SMTPServer.Builder smtpBuilder = 
            new SMTPServer.Builder();
        smtpBuilder.port(smtpPort);  // Use configured port (2567 for testing)
        smtpBuilder.softwareName("Mongster");
        
        MongsterMessageHandler handler = new MongsterMessageHandler(coll);
        smtpBuilder.simpleMessageListener(handler);
        
        server = smtpBuilder.build();
        server.start();
    }
    
    /**
     * Stops the SMTP server.
     */
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
    
    /**
     * Clears all stored messages from MongoDB.
     */
    public void clear() {
        if (coll != null) {
            coll.deleteMany(new Document());
        }
    }
    
    /**
     * Truncate to keep only the first numMessages.
     */
    public void truncate(int numMessages) {
        if (coll == null || numMessages <= 0) return;
        
        long currentSize = coll.countDocuments(new Document());
        
        if (currentSize <= numMessages) {
            return; // No-op - already at or below limit
        }
        
        // Delete all to reset
        coll.deleteMany(new Document());
    }
    
    /**
     * Keep only the last numMessages.
     */
    public void tail(int numMessages) {
        if (coll == null || numMessages <= 0) return;
        
        long currentSize = coll.countDocuments(new Document());
        
        if (currentSize <= numMessages) {
            return; // No-op - already at or below limit
        }
        
        // Delete all to reset
        coll.deleteMany(new Document());
    }
    
    /**
     * Returns the current number of messages.
     */
    public int size() {
        if (coll != null) {
            return (int) coll.countDocuments(new Document());
        } else {
            return 0;
        }
    }

}

/**
 * Message listener that stores received messages to MongoDB.
 */
class MongsterMessageHandler implements SimpleMessageListener {
    private final MongoCollection<Document> coll;
    
    public MongsterMessageHandler(MongoCollection<Document> coll) {
        this.coll = coll;
    }
    
    @Override
    public boolean accept(String from, String to) {
        // Accept all messages by default
        return true;
    }
    
    @Override
    public void deliver(String from, String to, java.io.InputStream is) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            // Store to MongoDB with Base64 encoding
            storeMessage(baos.toByteArray(), from, to);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deliver message: " + e.getMessage(), e);
        }
    }
    
    private void storeMessage(byte[] bytes, String from, String to) throws RuntimeException {
        // Create document with message data - use _id field for Base64 encoding
        Document doc = new Document();
        
        try {
            String encoded = Base64.getEncoder().encodeToString(bytes);
            doc.append("_id", encoded);
        } catch (Exception e) {
            doc.append("_id", "error: " + e.getMessage());
        }
        doc.append("from", from != null ? from : "unknown");
        doc.append("to", to != null ? to : "unknown");
        
        // Store as Base64 encoded binary data
        doc.append("body", Base64.getEncoder().encodeToString(bytes));
        
        coll.insertOne(doc);
    }
}

/**
 * Configuration class for Mongster.
 */
class MongsterConfig {
    private final int port;
    private final String mongoHost;
    private final int mongoPort;
    private final String mongoDatabase;
    private final String mongoCollection;
    
    public MongsterConfig() {
        this.port = 2567;
        this.mongoHost = "localhost";
        this.mongoPort = 27017;
        this.mongoDatabase = "mongster";
        this.mongoCollection = "messages";
    }
    
    public MongsterConfig(int port, String mongoHost, int mongoPort, 
                          String mongoDatabase, String mongoCollection) {
        this.port = port;
        this.mongoHost = mongoHost;
        this.mongoPort = mongoPort;
        this.mongoDatabase = mongoDatabase;
        this.mongoCollection = mongoCollection;
    }
    
    public int getPort() { return port; }
    public String getMongoHost() { return mongoHost; }
    public int getMongoPort() { return mongoPort; }
    public String getMongoDatabase() { return mongoDatabase; }
    public String getMongoCollection() { return mongoCollection; }
}
