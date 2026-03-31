package bftsmart.demo.incident;

public final class IdempotencyRecord implements CanonicalWritable {

    private final String requestId;
    private final int actionCode;
    private final byte[] semanticDigest;
    private final String reportId;
    private final String shipId;

    public IdempotencyRecord(String requestId, int actionCode, byte[] semanticDigest, String reportId, String shipId) {
        this.requestId = requestId == null ? "" : requestId;
        this.actionCode = actionCode;
        this.semanticDigest = semanticDigest == null ? new byte[0] : semanticDigest.clone();
        this.reportId = reportId == null ? "" : reportId;
        this.shipId = shipId == null ? "" : shipId;
    }

    public String getRequestId() {
        return requestId;
    }

    public int getActionCode() {
        return actionCode;
    }

    public byte[] getSemanticDigest() {
        return semanticDigest.clone();
    }

    public String getReportId() {
        return reportId;
    }

    public String getShipId() {
        return shipId;
    }

    @Override
    public void encode(CanonicalEncoder encoder) {
        encoder.writeString(requestId);
        encoder.writeInt(actionCode);
        encoder.writeByteArray(semanticDigest);
        encoder.writeString(reportId);
        encoder.writeString(shipId);
    }

    public static IdempotencyRecord decode(CanonicalDecoder decoder) {
        return new IdempotencyRecord(
                decoder.readString(),
                decoder.readInt(),
                decoder.readByteArray(),
                decoder.readString(),
                decoder.readString());
    }
}
