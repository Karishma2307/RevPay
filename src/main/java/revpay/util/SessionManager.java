package revpay.util;

public class SessionManager {

    
    private static final long TIMEOUT_MS = 5 * 60 * 1000;

    private long lastActivityTime;

    public SessionManager() {
        touch();
    }

    public void touch() {
        lastActivityTime = System.currentTimeMillis();
    }

    public boolean isExpired() {
        long now = System.currentTimeMillis();
        return (now - lastActivityTime) > TIMEOUT_MS;
    }
}
