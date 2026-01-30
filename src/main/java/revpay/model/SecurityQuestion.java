package revpay.model;

public class SecurityQuestion {

    private long qId;
    private String question;

    public SecurityQuestion() {}

    public SecurityQuestion(long qId, String question) {
        this.qId = qId;
        this.question = question;
    }

    public long getqId() {
        return qId;
    }

    public void setqId(long qId) {
        this.qId = qId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}