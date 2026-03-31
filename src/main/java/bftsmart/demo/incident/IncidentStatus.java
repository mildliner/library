package bftsmart.demo.incident;

public enum IncidentStatus {

    PENDING(1),
    VERIFIED(2),
    EXPIRED(3);

    private final int code;

    IncidentStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static IncidentStatus fromCode(int code) {
        for (IncidentStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported incident status code: " + code);
    }
}
