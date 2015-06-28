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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.subethamail.smtp.util.Base64;

import org.bson.Document;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

/**
 * Extends {@link Wiser} to persist received messages to a MongoDB database.
 * Constructor arguments allow the MongoDB configuration to be set as well
 * as whether or not received messages will be cached in memory (the default
 * behavior matching what Wiser does).  If {@code inMemory} is set to false,
 * {@link #getMessages()} and {@link #dumpMessages(java.io.PrintStream)} always
 * return empty lists.
 * <p>
 * The {@link #clear()} and {@link #truncate(int)} methods affect both the
 * in-memory cache and the MongoDB collection.</p>
 * <p>
 * Messages are stored in Mongo with the following attributes: <pre>
 *   envelopeSender      sender
 *   envelopeReceiver    receiver
 *   body                message content
 *   bytes64             Base64 raw bytes of the entire message (headers and content)
 *   sequenceNumber      order of message receipt since last clear
 * All headers are also appended to message documents with header names as keys.
 * </pre></p>
 *
 */
public class Mongster extends Wiser {
    
    /**
     * Creates a Mongster that stores received messages in memory and uses
     * the default MongoDB configuration (localhost, standard port, "mongster"
     * database name, "messages" collection name).
     */
    public Mongster() {
        this(new MongsterConfig());
    }
    
    /**
     * Creates a Mongster with the provided {@link MongsterConfig}.
     * 
     * @param config configuration settings
     */
    public Mongster(MongsterConfig config) {
        super();
        setPort(config.getSmtpPort());
        messages = new PersistedMessageList(config.isInMemory(), config.getMongoHost(), config.getMongoPort(),
                                            config.getMongoDatabase(), config.getMongoCollection());
    }
    
    /**
     * Returns the list of WiserMessages.
     * <p>
     * The number of mail transactions and the number of mails may be different.
     * If a message is received with multiple recipients in a single mail
     * transaction, then the list will contain more WiserMessage instances, one
     * for each recipient.</p>
     * <p>
     * If {@code false} is provided as an (optional) {@code inMemory} constructor
     * argument when creating a {@code Mongster}, this method will always return
     * an empty list.</p>
     * 
     * @return the list of messages received by Mongster since startup or {@link #clear()}
     */
    @Override
    public List<WiserMessage> getMessages() {
        return ((PersistedMessageList) messages).list();
    }
    
    public void clear() {
        messages.clear();
    }
    
    public void truncate(int numMessages) {
        ((PersistedMessageList) messages).truncate(numMessages);
    }
    
    public void tail(int numMessages) {
        ((PersistedMessageList) messages).tail(numMessages);
    }
    
    
    /** Starts up the server. */
    public static void main(String[] args) throws Exception {
        Mongster mongster = new Mongster();
        mongster.start();
    }
    
    /**
     * Subclass ArrayList to make {@link #add(WiserMessage)} also add to a MongoDB
     * collection. Also overrides {@link #clear()} to remove all documents from Mongo
     * and adds {@link #truncate(int)} and {@link #tail(int)} methods. Stores messages
     * in Mongo using headers as keys and puts body under "body".
     * <p>
     * To make sure json names are valid and to simplify client code, header names
     * are lower-cased and dashes are removed.</p>
     * <p>
     * THIS CLASS IS NOT INTENDED FOR REUSE. Only the methods actually used by
     * the parent class are correctly overridden.  List methods such as iterators
     * and indexed accessors / mutators act only on the in-memory list. </p>
     * 
     */
    static class PersistedMessageList extends ArrayList<WiserMessage> {
        private static final long serialVersionUID = 1L;
        
        private MongoDatabase db;
        private final MongoClient mongoClient;
        private final String mongoCollection;
        private final boolean inMemory;
        
        /**
         * Sequence numbers attached to received messages in MongoDB.
         */
        private long sequenceNumber = 0;
        
        public PersistedMessageList(boolean inMemory, String mongoHost, int mongoPort,
                                    String mongoDatabase, String mongoCollection) {
            this.mongoCollection = mongoCollection;
            mongoClient = new MongoClient(mongoHost);
            try {
                db = mongoClient.getDatabase(mongoDatabase);
            } catch (Exception ex) {
                mongoClient.close();
            }
            this.inMemory = inMemory;
        }

        @Override
        public boolean add(WiserMessage message) {
            synchronized (this) {
                if (!inMemory || super.add(message)) { // Short-circuit -> no add if inMemory is false
                    final MongoCollection<Document> coll = db.getCollection(mongoCollection); 
                    Document doc = new Document();
                    List<MailHeader> headers = null;
                    try {
                        headers = getHeadersFromMimeMessage(message.getMimeMessage());
                    } catch (MessagingException e) {
                        e.printStackTrace();
                    }
                    for (MailHeader header : headers) {
                        doc.append(fixHeaderName(header.name), header.value);
                    }
                    // Add message body FIXME: verify the content is text/plain and if not, handle specially
                    try {
                        doc.append("body", message.getMimeMessage().getContent());
                    } catch (IOException | MessagingException e) {
                        e.printStackTrace();
                    }
                    // Add WiserMessage attributes
                    doc.append("envelopeSender", message.getEnvelopeSender());
                    doc.append("envelopeReceiver", message.getEnvelopeReceiver());
                    doc.append("bytes64", Base64.encodeToString(message.getData(),true));
                    doc.append("sequenceNumber", sequenceNumber++);
                    coll.insertOne(doc);
                    return true;
                } else {
                    return false;
                }
            }
        }
        
        @Override
        public void clear() {
            synchronized (this) {
                sequenceNumber = 0;
                if (inMemory) {
                    super.clear();
                }
                final MongoCollection<Document> coll = db.getCollection("messages");
                coll.deleteMany(new Document());
            }
        }
        
        /**
         * Returns the messages stored in memory - empty if {@code inMemory}
         * is false.
         * 
         * @return {@code WiserMessages} stored in memory.
         */
        public List<WiserMessage> list() {
            if (inMemory) {
                return this;
            } else {
               return new ArrayList<WiserMessage>();
            }
        }
        
        /**
         * The number of received messages since last {@link #clear()}
         * or {@link #truncate(long)}.
         */ 
        @Override
        public int size() {
            if (inMemory) {
                return super.size();
            } else {
                return (int) db.getCollection("messages").count();
            }
        }
        
        /**
         * Truncate the list to {@code numMessages}. Drops all messages after
         * message number {@code numMessages}.  Use this method to keep the
         * earliest messages.
         * 
         * @param numMessages the number of messages to retain in the list.
         */
        public void truncate(int numMessages) {
            final int currentSize = size();
            if (numMessages >= currentSize) {
                return; //No-op if we are under the limit
            }
            synchronized(this) {
                if (inMemory) {
                    super.removeRange(numMessages, currentSize);
                }
                db.getCollection("messages").deleteMany(Filters.gt("sequenceNumber", numMessages - 1));
                sequenceNumber = numMessages - 1;
            }
        }
        
        /**
         * Drops all but the last {@code numMessages} messages.  Use this method
         * to keep the most recent messages.
         * 
         * @param numMessages the number of messages to retain in the list.
         */
        public void tail(int numMessages) {
            final int currentSize = size();
            if (numMessages >= currentSize) {
                return; //No-op if we are under the limit
            }
            synchronized(this) {
                final int toCut = currentSize - numMessages;
                if (inMemory) {
                    super.removeRange(0, toCut);
                }
                final MongoCollection<Document> coll = db.getCollection("messages");
                coll.deleteMany(Filters.lt("sequenceNumber", toCut));
                // Renumber - subtract toCut from every sequence number
                coll.updateMany(new Document(), new Document("$inc",
                                                  new Document("sequenceNumber", -toCut)));
                sequenceNumber = numMessages - 1;
            }
        }

        /**
         * Converts header name to all lower case and eliminates embedded dashes.
         * 
         * @param headerName header name to fix
         * @return lower-case, dash-free header name
         */
        private static String fixHeaderName(String headerName) {
            return headerName.toLowerCase().replace("-", "");  
        }
        
        /**
         * Extracts headers as MailHeader instances from a MimeMessage.
         * 
         * @param message MimeMessage to get headers from
         * @return list of MailHeaders
         * 
         * @throws MessagingException
         */
        private List<MailHeader> getHeadersFromMimeMessage(MimeMessage message) throws MessagingException {
            ArrayList<MailHeader> headerList = new ArrayList<MailHeader>();
            @SuppressWarnings("unchecked")
            Enumeration<Header> headers = message.getAllHeaders();
            while (headers.hasMoreElements()) {
                final Header header = headers.nextElement();
                headerList.add(new MailHeader(header.getName(),header.getValue()));
            }
            return headerList;
        }
    }

    /**
     * MIME header as name/value pair of strings.
     */
    static class MailHeader {
        final String name;
        final String value;
        public MailHeader(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

}
