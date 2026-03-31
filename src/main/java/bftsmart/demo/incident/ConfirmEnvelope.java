package bftsmart.demo.incident;

public final class ConfirmEnvelope implements CanonicalWritable {

    private final int schemaVersion;
    private final String action;
    private final byte[] reportHash;
    private final String reportId;
    private final String confirmerShipId;
    private final byte[] witnessEvidenceRoot;
    private final int decisionCode;
    private final String requestId;
    private final long issuedAtMs;
    private final long expiresAtMs;
    private final String signatureAlgorithm;
    private final byte[] signatureBytes;

    public ConfirmEnvelope(
            int schemaVersion,
            String action,
            byte[] reportHash,
            String reportId,
            String confirmerShipId,
            byte[] witnessEvidenceRoot,
            int decisionCode,
            String requestId,
            long issuedAtMs,
            long expiresAtMs,
            String signatureAlgorithm,
            byte[] signatureBytes) {
        this.schemaVersion = schemaVersion;
        this.action = action == null ? "" : action;
        this.reportHash = reportHash == null ? new byte[0] : reportHash.clone();
        this.reportId = reportId == null ? "" : reportId;
        this.confirmerShipId = confirmerShipId == null ? "" : confirmerShipId;
        this.witnessEvidenceRoot = witnessEvidenceRoot == null ? new byte[0] : witnessEvidenceRoot.clone();
        this.decisionCode = decisionCode;
        this.requestId = requestId == null ? "" : requestId;
        this.issuedAtMs = issuedAtMs;
        this.expiresAtMs = expiresAtMs;
        this.signatureAlgorithm = signatureAlgorithm == null ? "" : signatureAlgorithm;
        this.signatureBytes = signatureBytes == null ? new byte[0] : signatureBytes.clone();
    }

    public static ConfirmEnvelope sign(
            byte[] reportHash,
            String reportId,
            String confirmerShipId,
            byte[] witnessEvidenceRoot,
            int decisionCode,
            String requestId,
            long issuedAtMs,
            long expiresAtMs,
            ShipIdentity identity) {
        byte[] tbs = createTbs(
                IncidentProtocol.SCHEMA_VERSION,
                reportHash,
                reportId,
                confirmerShipId,
                witnessEvidenceRoot,
                decisionCode,
                requestId,
                issuedAtMs,
                expiresAtMs);
        return new ConfirmEnvelope(
                IncidentProtocol.SCHEMA_VERSION,
                IncidentProtocol.ACTION_CONFIRM,
                reportHash,
                reportId,
                confirmerShipId,
                witnessEvidenceRoot,
                decisionCode,
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

    public String getConfirmerShipId() {
        return confirmerShipId;
    }

    public byte[] getWitnessEvidenceRoot() {
        return witnessEvidenceRoot.clone();
    }

    public int getDecisionCode() {
        return decisionCode;
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
        return createTbs(
                schemaVersion,
                reportHash,
                reportId,
                confirmerShipId,
                witnessEvidenceRoot,
                decisionCode,
                requestId,
                issuedAtMs,
                expiresAtMs);
    }

    public static byte[] createTbs(
            int schemaVersion,
            byte[] reportHash,
            String reportId,
            String confirmerShipId,
            byte[] witnessEvidenceRoot,
            int decisionCode,
            String requestId,
            long issuedAtMs,
            long expiresAtMs) {
        CanonicalEncoder encoder = new CanonicalEncoder();
        encoder.writeInt(schemaVersion);
        encoder.writeString(IncidentProtocol.ACTION_CONFIRM);
        encoder.writeByteArray(reportHash);
        encoder.writeString(reportId);
        encoder.writeString(confirmerShipId);
        encoder.writeByteArray(witnessEvidenceRoot);
        encoder.writeInt(decisionCode);
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
        encoder.writeString(confirmerShipId);
        encoder.writeByteArray(witnessEvidenceRoot);
        encoder.writeInt(decisionCode);
        encoder.writeString(requestId);
        encoder.writeLong(issuedAtMs);
        encoder.writeLong(expiresAtMs);
        encoder.writeString(signatureAlgorithm);
        encoder.writeByteArray(signatureBytes);
    }

    public static ConfirmEnvelope decode(CanonicalDecoder decoder) {
        return new ConfirmEnvelope(
                decoder.readInt(),
                decoder.readString(),
                decoder.readByteArray(),
                decoder.readString(),
                decoder.readString(),
                decoder.readByteArray(),
                decoder.readInt(),
                decoder.readString(),
                decoder.readLong(),
                decoder.readLong(),
                decoder.readString(),
                decoder.readByteArray());
    }
}
