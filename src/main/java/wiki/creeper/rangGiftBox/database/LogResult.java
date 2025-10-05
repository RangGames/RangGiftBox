package wiki.creeper.rangGiftBox.database;

public enum LogResult {
    EXPIRED(0),
    CLAIMED(1),
    SENT(2);

    private final int value;

    LogResult(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}