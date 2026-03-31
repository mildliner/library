package bftsmart.demo.incident;

public enum IncidentRequestType {

    SUBMIT(1),
    CONFIRM(2),
    GET_REPORT(3),
    LIST_REPORTS(4),
    GET_LEDGER_HEAD(5);

    private final int code;

    IncidentRequestType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static IncidentRequestType fromCode(int code) {
        for (IncidentRequestType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported incident request type code: " + code);
    }
}
