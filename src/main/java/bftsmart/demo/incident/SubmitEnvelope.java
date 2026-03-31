package bftsmart.demo.incident;

public final class SubmitEnvelope implements CanonicalWritable {

    private final int schemaVersion;
    private final String action;
    private final byte[] reportHash;
    private final String reportId;
    private final String reporterShipId;
    private final String requestId;
    private final long issuedAtMs;
    private final long expiresAtMs;
    private final String signatureAlgorithm;
    private final byte[] signatureBytes;

    public SubmitEnvelope(
            int schemaVersion,
            String action,
            byte[] reportHash,
            String reportId,
            String reporterShipId,
            String requestId,
            long issuedAtMs,
            long expiresAtMs,
            String signatureAlgorithm,
            byte[] signatureBytes) {
        this.schemaVersion = schemaVersion;
        this.action = action == null ? "" : action;
        this.reportHash = reportHash == null ? new byte[0] : reportHash.clone();
        this.reportId = reportId == null ? "" : reportId;
        this.reporterShipId = reporterShipId == null ? "" : reporterShipId;
        this.requestId = requestId == null ? "" : requestId;
        this.issuedAtMs = issuedAtMs;
        this.expiresAtMs = expiresAtMs;
        this.signatureAlgorithm = signatureAlgorithm == null ? "" : signatureAlgorithm;
        this.signatureBytes = signatureBytes == null ? new byte[0] : signatureBytes.clone();
    }

    public static SubmitEnvelope sign(
            IncidentPayload payload,
            String requestId,
            long issuedAtMs,
            long expiresAtMs,
            ShipIdentity identity) {
        byte[] reportHash = payload.reportHash();
        byte[] tbs = createTbs(
                payload.getSchemaVersion(),
                reportHash,
                payload.getReportId(),
                payload.getReporterShipId(),
                requestId,
                issuedAtMs,
                expiresAtMs);
        return new SubmitEnvelope(
                payload.getSchemaVersion(),
                IncidentProtocol.ACTION_SUBMIT,
                reportHash,
                payload.getReportId(),
                payload.getReporterShipId(),
                requestId,
                issuedAtMs,
                expiresAtMs,
                IncidentProtocol.SIGNATURE_ALGORITHM,
                CryptoUtils.sign(identity.getPrivateKey(), tbs));
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getAction() {
        return action;
    }

    public byte[] getReportHash() {
        return reportHash.clone();
    }

    public String getReportId() {
        return reportId;
    }

    public String getReporterShipId() {
        return reporterShipId;
    }

    public String getRequestId() {
        return requestId;
    }

    public long getIssuedAtMs() {
        return issuedAtMs;
    }

    public long getExpiresAtMs() {
        return expiresAtMs;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public byte[] getSignatureBytes() {
        return signatureBytes.clone();
    }

    public byte[] toBeSignedBytes() {
        return createTbs(schemaVersion, reportHash, reportId, reporterShipId, requestId, issuedAtMs, expiresAtMs);
    }

    public static byte[] createTbs(
            int schemaVersion,
            byte[] reportHash,
            String reportId,
            String reporterShipId,
            String requestId,
            long issuedAtMs,
            long expiresAtMs) {
        CanonicalEncoder encoder = new CanonicalEncoder();
        encoder.writeInt(schemaVersion);
        encoder.writeString(IncidentProtocol.ACTION_SUBMIT);
        encoder.writeByteArray(reportHash);
        encoder.writeString(reportId);
        encoder.writeString(reporterShipId);
        encoder.writeString(requestId);
        encoder.writeLong(issuedAtMs);
        encoder.writeLong(expiresAtMs);
        return encoder.toByteArray();
    }

    @Override
    public void encode(CanonicalEncoder encoder) {
        encoder.writeInt(schemaVersion);
        encoder.writeString(action);
        encoder.writeByteArray(reportHash);
        encoder.writeString(reportId);
        encoder.writeString(reporterShipId);
        encoder.writeString(requestId);
        encoder.writeLong(issuedAtMs);
        encoder.writeLong(expiresAtMs);
        encoder.writeString(signatureAlgorithm);
        encoder.writeByteArray(signatureBytes);
    }

    public static SubmitEnvelope decode(CanonicalDecoder decoder) {
        return new SubmitEnvelope(
                decoder.readInt(),
                decoder.readString(),
                decoder.readByteArray(),
                decoder.readString(),
                decoder.readString(),
                decoder.readString(),
                decoder.readLong(),
                decoder.readLong(),
                decoder.readString(),
                decoder.readByteArray());
    }
}
