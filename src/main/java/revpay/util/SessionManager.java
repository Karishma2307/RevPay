package revpay.util;

public class SessionManager {

    // ✅ 1S minutes default inactivity timeout (change if you want)
    private static final long TIMEOUT_MS = 1 * 60 * 1000;

    private long lastActivityMs;

    public SessionManager() {
        touch();
    }

    // call this on every user action
    public void touch() {
        lastActivityMs = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - lastActivityMs > TIMEOUT_MS;
    }

    public long secondsRemaining() {
        long remaining = TIMEOUT_MS - (System.currentTimeMillis() - lastActivityMs);
        if (remaining < 0) remaining = 0;
        return remaining / 1000;
    }
}
