import org.subethamail.wiser.WiserMessage;
import jakarta.mail.Session;

public class WiserTest {
    public static void main(String[] args) throws Exception {
        Session session = Session.getInstance(new java.util.Properties());
        
        System.out.println("WiserMessage class: " + WiserMessage.class.getName());
        
        try {
            // Try new() constructor with arguments
            var m = new WiserMessage(session, null, null, null);
            System.out.println("new(session, from, to, bytes) succeeded");
        } catch (Exception e) {
            System.out.println("new() failed: " + e.getMessage());
        }
    }
}
