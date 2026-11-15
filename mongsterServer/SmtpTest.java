import org.subethamail.smtp.server.SMTPServer;

public class SmtpTest {
    public static void main(String[] args) throws Exception {
        SMTPServer.Builder b = new SMTPServer.Builder();
        b.port(25);
        b.softwareName("test");
        b.simpleMessageListener(new org.subethamail.smtp.helper.SimpleMessageListener() {
            @Override
            public boolean accept(String from, String to) { return true; }
            
            @Override
            public void deliver(String from, String to, java.io.InputStream is) {
                // handle
            }
        });
        
        SMTPServer result = b.build();
        System.out.println("build() returned: " + (result == null ? "null" : "SMTPServer instance"));
    }
}
