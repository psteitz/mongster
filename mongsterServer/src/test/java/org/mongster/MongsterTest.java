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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mongster.Mongster;
import org.subethamail.wiser.WiserMessage;

import com.mongodb.MongoClient;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Unit tests for Mongster.  Most test cases are adapted from Wiser unit tests.
 * 
 * NOTE: there must be a MongoDB server running with the given config for these
 * test cases to run.
 * 
 * FIXME: externalize the MongoDB config.
 * 
 */
public class MongsterTest {

    public static final int PORT = 2566;
    public static final String MONGO_HOST = "localhost";
    public static final String MONGO_DATABASE = "mongster";
    public static final String MONGO_COLLECTION = "messages";
    public static final int MONGO_PORT = 27017;
    
    private MongoDatabase db;
    private MongoClient mongoClient;

    protected Mongster mongster;
    protected Session session;

    @Before
    public void setUp() throws Exception {
        mongoClient = new MongoClient(MONGO_HOST);
        try {
            db = mongoClient.getDatabase(MONGO_DATABASE);
        } catch (Exception ex) {
            mongoClient.close();
        }
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", "localhost");
        props.setProperty("mail.smtp.port", Integer.toString(PORT));
        session = Session.getInstance(props);
        mongster = new Mongster();
        mongster.setPort(PORT);
        mongster.start();
        mongster.clear();
    }

    @After
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
        Assert.assertEquals(1, mongster.getMessages().size());
        String[] receivedHeaders = mongster.getMessages().get(0).getMimeMessage().getHeader("Received");
        Assert.assertEquals(1, receivedHeaders.length);
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        Assert.assertEquals(1,coll.count());
        Document doc = coll.find().first();
        Assert.assertTrue(doc.getString("received").indexOf("from") >= 0);
        Assert.assertTrue(doc.getString("from").indexOf("someone@somewhereelse.com") >= 0);
        Assert.assertTrue(doc.getString("to").indexOf("anyone@anywhere.com") >= 0);
        Assert.assertTrue(doc.getString("body").indexOf("body") >= 0);
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

        Assert.assertEquals(2, mongster.getMessages().size());
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

        Assert.assertEquals(2, mongster.getMessages().size());

        Assert.assertEquals("barf", mongster.getMessages().get(0)
            .getMimeMessage().getSubject());
        Assert.assertEquals("barf", mongster.getMessages().get(1)
            .getMimeMessage().getSubject());
    }

    @Test
    public void testUtf8EightBitMessage()
        throws Exception {
        // Beware editor/compiler character encoding issues; safest to put
        // unicode escapes here

        String body = "\u00a4uro ma\u00f1ana\r\n";
        this.testEightBitMessage(body, "UTF-8");

        Assert.assertEquals(body, mongster.getMessages().get(0)
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

        Assert.assertEquals(body, mongster.getMessages().get(0)
            .getMimeMessage().getContent());
    }

    @Test
    public void testIso885915EightBitMessage()
        throws Exception {
        // Beware editor/compiler character encoding issues; safest to put
        // unicode escapes here

        String body = "\u0080uro\r\n"; // should be the euro symbol
        this.testEightBitMessage(body, "ISO-8859-15");

        Assert.assertEquals(body, mongster.getMessages().get(0)
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

        Assert.assertEquals(body, mongster.getMessages().get(0)
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

        Assert.assertTrue(Arrays.equals(body, tmp.toByteArray()));
    }
    
    @Test
    public void testClear() throws Exception {
        sendMessageSequence(10);
        Assert.assertEquals(10, mongster.getMessages().size());
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        Assert.assertEquals(10,coll.count());
        mongster.clear();
        Assert.assertEquals(0, mongster.getMessages().size());
        coll = db.getCollection(MONGO_COLLECTION);
        Assert.assertEquals(0,coll.count());
    }
    
    @Test
    public void testTruncate() throws Exception {
        sendMessageSequence(10);
        Assert.assertEquals(10, mongster.getMessages().size());
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        Assert.assertEquals(10,coll.count());
        mongster.truncate(5);
        final List<WiserMessage> messageList = mongster.getMessages();
        Assert.assertEquals(5, messageList.size());
        Assert.assertTrue(messageList.get(4).getMimeMessage().getSubject().equals("barf4"));
        coll = db.getCollection(MONGO_COLLECTION);
        Assert.assertEquals(5,coll.count());
        Document doc  = coll.find(exists("sequenceNumber")).sort(descending("sequenceNumber")).first();
        Assert.assertTrue(doc.getString("subject").equals("barf4"));
    }
    
    @Test
    public void testTail() throws Exception {
        sendMessageSequence(10);
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        mongster.tail(5);
        final List<WiserMessage> messageList = mongster.getMessages();
        Assert.assertEquals(5, messageList.size());
        Assert.assertTrue(messageList.get(4).getMimeMessage().getSubject().equals("barf9"));
        coll = db.getCollection(MONGO_COLLECTION);
        Assert.assertEquals(5,coll.count());
        Document doc  = coll.find(exists("sequenceNumber")).sort(descending("sequenceNumber")).first();
        Assert.assertTrue(doc.getString("subject").equals("barf9"));
        long val = doc.getLong("sequenceNumber");
        Assert.assertEquals(4, val );
    }
    
    @Test
    public void testDecodeMongoMimeMessage() throws Exception {
        sendMessageSequence(1);
        MongoCollection<Document> coll = db.getCollection(MONGO_COLLECTION);
        Assert.assertEquals(1,coll.count());
        Document doc  = coll.find().first();
        Base64 codec = new Base64();
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()),
                                                  new ByteArrayInputStream(codec.decode(doc.getString("bytes64"))));
        Assert.assertTrue(mimeMessage.getContent().toString().startsWith("text0"));
        Assert.assertTrue(mimeMessage.getSubject().toString().equals("barf0"));
        Assert.assertTrue(mimeMessage.getFrom()[0].toString().equals("someone@somewhereelse.com"));
        Assert.assertTrue(mimeMessage.getAllRecipients()[0].toString().equals("anyone@anywhere.com"));
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
