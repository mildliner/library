package bftsmart.demo.incident;

public final class IncidentPayload implements CanonicalWritable {

    private final int schemaVersion;
    private final String reportId;
    private final String reporterShipId;
    private final long eventTimeMs;
    private final long reportedAtMs;
    private final int latE7;
    private final int lonE7;
    private final int incidentTypeCode;
    private final int severityCode;
    private final byte[] reporterEvidenceRoot;
    private final String reporterKeyId;

    public IncidentPayload(
            int schemaVersion,
            String reportId,
            String reporterShipId,
            long eventTimeMs,
            long reportedAtMs,
            int latE7,
            int lonE7,
            int incidentTypeCode,
            int severityCode,
            byte[] reporterEvidenceRoot,
            String reporterKeyId) {
        this.schemaVersion = schemaVersion;
        this.reportId = reportId == null ? "" : reportId;
        this.reporterShipId = reporterShipId == null ? "" : reporterShipId;
        this.eventTimeMs = eventTimeMs;
        this.reportedAtMs = reportedAtMs;
        this.latE7 = latE7;
        this.lonE7 = lonE7;
        this.incidentTypeCode = incidentTypeCode;
        this.severityCode = severityCode;
        this.reporterEvidenceRoot = reporterEvidenceRoot == null ? new byte[0] : reporterEvidenceRoot.clone();
        this.reporterKeyId = reporterKeyId == null ? "" : reporterKeyId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getReportId() {
        return reportId;
    }

    public String getReporterShipId() {
        return reporterShipId;
    }

    public long getEventTimeMs() {
        return eventTimeMs;
    }

    public long getReportedAtMs() {
        return reportedAtMs;
    }

    public int getLatE7() {
        return latE7;
    }

    public int getLonE7() {
        return lonE7;
    }

    public int getIncidentTypeCode() {
        return incidentTypeCode;
    }

    public int getSeverityCode() {
        return severityCode;
    }

    public byte[] getReporterEvidenceRoot() {
        return reporterEvidenceRoot.clone();
    }

    public String getReporterKeyId() {
        return reporterKeyId;
    }

    public byte[] reportHash() {
        return CryptoUtils.sha256(CanonicalEncoder.encode(this));
    }

    @Override
    public void encode(CanonicalEncoder encoder) {
        encoder.writeInt(schemaVersion);
        encoder.writeString(reportId);
        encoder.writeString(reporterShipId);
        encoder.writeLong(eventTimeMs);
        encoder.writeLong(reportedAtMs);
        encoder.writeInt(latE7);
        encoder.writeInt(lonE7);
        encoder.writeInt(incidentTypeCode);
        encoder.writeInt(severityCode);
        encoder.writeByteArray(reporterEvidenceRoot);
        encoder.writeString(reporterKeyId);
    }

    public static IncidentPayload decode(CanonicalDecoder decoder) {
        return new IncidentPayload(
                decoder.readInt(),
                decoder.readString(),
                decoder.readString(),
                decoder.readLong(),
                decoder.readLong(),
                decoder.readInt(),
                decoder.readInt(),
                decoder.readInt(),
                decoder.readInt(),
                decoder.readByteArray(),
                decoder.readString());
    }
}
