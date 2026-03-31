package bftsmart.demo.incident;

public final class IncidentProtocol {

    public static final int SCHEMA_VERSION = 1;
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_CONFIRM = "CONFIRM";
    public static final String SIGNATURE_ALGORITHM = "SHA256withECDSA";
    public static final long DEFAULT_ALLOWED_CLOCK_SKEW_MS = 60_000L;
    public static final long DEFAULT_TTL_MS = 5L * 60L * 1000L;
    public static final int DECISION_CONFIRM = 1;

    private IncidentProtocol() {
    }
}
