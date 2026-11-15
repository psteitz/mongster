package org.mongster;
import org.bson.Document;

import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;


/**
 * Unit tests for Mongster.  Most test cases are adapted from Wiser unit tests.
 */
public class MongsterTest {

    private static GenericContainer mongoDb;
    private static Mongster mongster;

    @BeforeAll
    public static void setupOnce() throws Exception {
        mongoDb = new GenericContainer("mongo:7.0")
            .withExposedPorts(27017);
        mongoDb.start();

        String mongoUrl = "mongodb://" + mongoDb.getHost() + ":" + mongoDb.getMappedPort(27017);
        
        // Use a non-privileged port for testing SMTP
        mongster = new Mongster(new MongsterConfig(
            2567,  // Non-privileged port for testing
            mongoDb.getHost(),
            mongoDb.getMappedPort(27017),
            "mongster",
            "messages"
        ));
        mongster.start();  // Start SMTP server before tests
    }

    @AfterEach
    public void tearDown() throws Exception {
        mongster.clear();
    }

    /** Simple helper to create and send a message */
    private static void sendMessage(String from, String to, byte[] body) 
        throws Exception {
        // Configure Session to connect to Mongster on the configured port
        Properties props = new Properties();
        props.put("mail.smtp.host", "localhost");
        props.put("mail.smtp.port", "2567");  // Use testing port
        
        Session session = Session.getInstance(props);
        
        // Create simple text-based MimeMessage
        jakarta.mail.internet.MimeMessage msg = new jakarta.mail.internet.MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        
        InternetAddress address = new InternetAddress(to);
        msg.addRecipient(Message.RecipientType.TO, address);
        
        // Set plain text content directly
        String subject = "Test";
        msg.setSubject(subject);
        msg.setText(new String(body, java.nio.charset.StandardCharsets.UTF_8));
        
        jakarta.mail.Transport.send(msg);
    }

    @Test
    public void testBasicReceive() throws Exception {
        sendMessage("sender@local", "recipient@local", 
            "Hello World".getBytes());
        
        // Verify message was stored
        int size = mongster.size();
        org.junit.jupiter.api.Assertions.assertTrue(size > 0, "Message should be received");
    }

    @Test
    public void testBasicReceiveAndRetrieve() throws Exception {
        sendMessage("sender1@local", "recipient1@local", 
            "First message".getBytes());
        
        // Verify we can get messages back
        java.util.List<Document> messages = mongster.getMessages();
        org.junit.jupiter.api.Assertions.assertTrue(messages.size() >= 1);
    }

    @Test
    public void testBasicReceiveAndSendMultiple() throws Exception {
        sendMessage("sender@local", "recipient@local", 
            "Message 1".getBytes());
        org.junit.jupiter.api.Assertions.assertEquals(mongster.size(), 1, "Should have one message");
        
        sendMessage("sender@local", "recipient@local", 
            "Message 2".getBytes());
        org.junit.jupiter.api.Assertions.assertEquals(mongster.size(), 2, "Should have two messages");
    }

    @Test
    public void testClear() throws Exception {
        for (int i = 0; i < 10; i++) {
            sendMessage("sender@local", "recipient@local", 
                ("Message " + i).getBytes());
        }
        
        org.junit.jupiter.api.Assertions.assertEquals(mongster.size(), 10, "Should have ten messages");
        
        mongster.clear();
        org.junit.jupiter.api.Assertions.assertEquals(mongster.size(), 0, "Clear should remove all messages");
    }

}
