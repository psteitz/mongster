package org.mongster;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.codec.binary.Base64;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.subethamail.wiser.WiserMessage;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Unit tests for Mongster.  Most test cases are adapted from Wiser unit tests.
 * 
 * NOTE: a MongoDB server is started in a Docker container using Testcontainers,
 * so a Docker environment must be available, but no local MongoDB is required.
 */
public class MongsterTest {

    public static final int PORT = 2566;
    public static final String MONGO_DATABASE = "mongster";
    public static final String MONGO_COLLECTION = "messages";

    @SuppressWarnings("resource") // started in setUpClass and stopped in tearDownClass
    public static final GenericContainer<?> MONGO =
        new GenericContainer<>(DockerImageName.parse("mongo:7.0")).withExposedPorts(27017);

    @BeforeAll
    public static void setUpClass() {
        MONGO.start();
    }

    @AfterAll
    public static void tearDownClass() {
        MONGO.stop();
    }

    private MongoDatabase db;
    private MongoClient mongoClient;

    protected Mongster mongster;
    protected Session session;

    @BeforeEach
    public void setUp() throws Exception {
        String mongoHost = MONGO.getHost();
        int mongoPort = MONGO.getMappedPort(27017);
        mongoClient = MongoClients.create("mongodb://" + mongoHost + ":" + mongoPort);
        try {
            db = mongoClient.getDatabase(MONGO_DATABASE);
        } catch (Exception ex) {
            mongoClient.close();
        }
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", Integer.toString(PORT));
        session = Session.getInstance(props);
        MongsterConfig config = new MongsterConfig();
        config.setMongoHost(mongoHost);
        config.setMongoPort(mongoPort);
        config.setMongoDatabase(MONGO_DATABASE);
        config.setMongoCollection(MONGO_COLLECTION);
        config.setSmtpPort(PORT);
        mongster = new Mongster(config);
        mongster.start();
        mongster.clear();
    }

    @AfterEach
    public void tearDown() throws Exception {
        mongster.stop();
        mongster.clear();
        mongoClient.close();
    }

    @Test
    public void testReceivedHeader() throws Exception {
        MimeMessage message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress("anyone@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("barf");
        message.setText("body");
        Transport.send(message);
        Assertions.assertEquals(1, mongster.getMessages().size());
        String[] receivedHeaders = mongster.getMessages().get(0).getMimeMessage().getHeader("Received");
        Assertions.assertEquals(1, receivedHeaders.length);
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        Assertions.assertEquals(1,coll.countDocuments());
        Document doc = coll.find().first();
        Assertions.assertTrue(doc.getString("received").indexOf("from") >= 0);
        Assertions.assertTrue(doc.getString("from").indexOf("someone@somewhereelse.com") >= 0);
        Assertions.assertTrue(doc.getString("to").indexOf("anyone@anywhere.com") >= 0);
        Assertions.assertTrue(doc.getString("body").indexOf("body") >= 0);
    }

    @Test
    public void testMultipleRecipients()
        throws Exception {
        MimeMessage message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO,
                             new InternetAddress("anyone@anywhere.com"));
        message.addRecipient(Message.RecipientType.TO,
                             new InternetAddress("anyone2@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("barf");
        message.setText("body");

        Transport.send(message);

        Assertions.assertEquals(2, mongster.getMessages().size());
    }

    @Test
    public void testLargeMessage()
        throws Exception {
        MimeMessage message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO,
                             new InternetAddress("anyone@anywhere.com"));
        message.addRecipient(Message.RecipientType.TO,
                             new InternetAddress("anyone2@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("barf");
        message
            .setText("bodyalksdjflkasldfkasjldfkjalskdfjlaskjdflaksdjflkjasdlfkjl");

        Transport.send(message);

        Assertions.assertEquals(2, mongster.getMessages().size());

        Assertions.assertEquals("barf", mongster.getMessages().get(0)
            .getMimeMessage().getSubject());
        Assertions.assertEquals("barf", mongster.getMessages().get(1)
            .getMimeMessage().getSubject());
    }

    @Test
    public void testUtf8EightBitMessage()
        throws Exception {
        // Beware editor/compiler character encoding issues; safest to put
        // unicode escapes here

        String body = "\u00a4uro ma\u00f1ana\r\n";
        this.testEightBitMessage(body, "UTF-8");

        Assertions.assertEquals(body, mongster.getMessages().get(0)
            .getMimeMessage().getContent());
    }

    @Test
    public void testIso88591EightBitMessage()
        throws Exception {
        // Beware editor/compiler character encoding issues; safest to put
        // unicode escapes here

        String body = "ma\u00f1ana\r\n"; // spanish ene (ie, n with diacritical
                                         // tilde)
        this.testEightBitMessage(body, "ISO-8859-1");

        Assertions.assertEquals(body, mongster.getMessages().get(0)
            .getMimeMessage().getContent());
    }

    @Test
    public void testIso885915EightBitMessage()
        throws Exception {
        // Beware editor/compiler character encoding issues; safest to put
        // unicode escapes here

        String body = "\u0080uro\r\n"; // should be the euro symbol
        this.testEightBitMessage(body, "ISO-8859-15");

        Assertions.assertEquals(body, mongster.getMessages().get(0)
            .getMimeMessage().getContent());
    }

    private void testEightBitMessage(String body, String charset)
        throws Exception {
        MimeMessage message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO,
                             new InternetAddress("anyone@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("hello");
        message.setText(body, charset);
        message.setHeader("Content-Transfer-Encoding", "8bit");

        Transport.send(message);
    }

    @Test
    public void testIso2022JPEightBitMessage()
        throws Exception {
        String body = "\u3042\u3044\u3046\u3048\u304a\r\n"; // some Japanese
                                                            // letters
        this.testEightBitMessage(body, "iso-2022-jp");

        Assertions.assertEquals(body, mongster.getMessages().get(0)
            .getMimeMessage().getContent());
    }

    // @Test <- Mongo chokes on this
    public void testBinaryEightBitMessage()
        throws Exception {
        byte[] body = new byte[64];
        new Random().nextBytes(body);

        MimeMessage message = new MimeMessage(this.session);
        message.addRecipient(Message.RecipientType.TO,
                             new InternetAddress("anyone@anywhere.com"));
        message.setFrom(new InternetAddress("someone@somewhereelse.com"));
        message.setSubject("hello");
        message.setHeader("Content-Transfer-Encoding", "8bit");
        message
            .setDataHandler(new DataHandler(
                                            new ByteArrayDataSource(body,
                                                                    "application/octet-stream")));

        Transport.send(message);

        InputStream in = mongster.getMessages().get(0).getMimeMessage()
            .getInputStream();
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        byte[] buf = new byte[64];
        int n;
        while ((n = in.read(buf)) != -1) {
            tmp.write(buf, 0, n);
        }
        in.close();

        Assertions.assertTrue(Arrays.equals(body, tmp.toByteArray()));
    }
    
    @Test
    public void testClear() throws Exception {
        sendMessageSequence(10);
        Assertions.assertEquals(10, mongster.getMessages().size());
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        Assertions.assertEquals(10,coll.countDocuments());
        mongster.clear();
        Assertions.assertEquals(0, mongster.getMessages().size());
        coll = db.getCollection(MONGO_COLLECTION);
        Assertions.assertEquals(0,coll.countDocuments());
    }
    
    @Test
    public void testTruncate() throws Exception {
        sendMessageSequence(10);
        Assertions.assertEquals(10, mongster.getMessages().size());
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        Assertions.assertEquals(10,coll.countDocuments());
        mongster.truncate(5);
        final List<WiserMessage> messageList = mongster.getMessages();
        Assertions.assertEquals(5, messageList.size());
        Assertions.assertTrue(messageList.get(4).getMimeMessage().getSubject().equals("barf4"));
        coll = db.getCollection(MONGO_COLLECTION);
        Assertions.assertEquals(5,coll.countDocuments());
        Document doc  = coll.find(exists("sequenceNumber")).sort(descending("sequenceNumber")).first();
        Assertions.assertTrue(doc.getString("subject").equals("barf4"));
    }
    
    @Test
    public void testTail() throws Exception {
        sendMessageSequence(10);
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        mongster.tail(5);
        final List<WiserMessage> messageList = mongster.getMessages();
        Assertions.assertEquals(5, messageList.size());
        Assertions.assertTrue(messageList.get(4).getMimeMessage().getSubject().equals("barf9"));
        coll = db.getCollection(MONGO_COLLECTION);
        Assertions.assertEquals(5,coll.countDocuments());
        Document doc  = coll.find(exists("sequenceNumber")).sort(descending("sequenceNumber")).first();
        Assertions.assertTrue(doc.getString("subject").equals("barf9"));
        long val = doc.getLong("sequenceNumber");
        Assertions.assertEquals(4, val );
    }
    
    @Test
    public void testDecodeMongoMimeMessage() throws Exception {
        sendMessageSequence(1);
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        Assertions.assertEquals(1,coll.countDocuments());
        Document doc  = coll.find().first();
        Base64 codec = new Base64();
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
                                                  new ByteArrayInputStream(codec.decode(doc.getString("bytes64"))));
        Assertions.assertTrue(mimeMessage.getContent().toString().startsWith("text0"));
        Assertions.assertTrue(mimeMessage.getSubject().toString().equals("barf0"));
        Assertions.assertTrue(mimeMessage.getFrom()[0].toString().equals("someone@somewhereelse.com"));
        Assertions.assertTrue(mimeMessage.getAllRecipients()[0].toString().equals("anyone@anywhere.com"));
    }
    
    private void sendMessageSequence(int numMessages) throws Exception {
        for (int i = 0; i < numMessages; i++) {
            MimeMessage message = new MimeMessage(this.session);
            message.addRecipient(Message.RecipientType.TO,
                                 new InternetAddress("anyone@anywhere.com"));
            message.setFrom(new InternetAddress("someone@somewhereelse.com"));
            message.setSubject("barf" + i);
            message.setText("text" + i);
            Transport.send(message);
        }
    }
}
