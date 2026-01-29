package revpay.model;

import java.util.Date;

public class Notification {

    private long notificationId;
    private long userId;
    private String type;   // TRANSACTION, ALERT, REQUEST
    private String title;
    private String message;
    private boolean read;
    private Date createdAt;

    public long getNotificationId() {
        return notificationId;
    }
    public void setNotificationId(long notificationId) {
        this.notificationId = notificationId;
    }
    public long getUserId() {
        return userId;
    }
    public void setUserId(long userId) {
        this.userId = userId;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public boolean isRead() {
        return read;
    }
    public void setRead(boolean read) {
        this.read = read;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
