package bftsmart.demo.incident;

public final class IncidentRequest implements CanonicalWritable {

    private final int schemaVersion;
    private final IncidentRequestType type;
    private final String reportId;
    private final int requiredConfirmations;
    private final IncidentPayload payload;
    private final SubmitEnvelope submitEnvelope;
    private final ConfirmEnvelope confirmEnvelope;

    private IncidentRequest(
            int schemaVersion,
            IncidentRequestType type,
            String reportId,
            int requiredConfirmations,
            IncidentPayload payload,
            SubmitEnvelope submitEnvelope,
            ConfirmEnvelope confirmEnvelope) {
        this.schemaVersion = schemaVersion;
        this.type = type;
        this.reportId = reportId == null ? "" : reportId;
        this.requiredConfirmations = requiredConfirmations;
        this.payload = payload;
        this.submitEnvelope = submitEnvelope;
        this.confirmEnvelope = confirmEnvelope;
    }

    public static IncidentRequest submit(IncidentPayload payload, SubmitEnvelope submitEnvelope, int requiredConfirmations) {
        return new IncidentRequest(
                IncidentProtocol.SCHEMA_VERSION,
                IncidentRequestType.SUBMIT,
                payload.getReportId(),
                requiredConfirmations,
                payload,
                submitEnvelope,
                null);
    }

    public static IncidentRequest confirm(ConfirmEnvelope confirmEnvelope) {
        return new IncidentRequest(
                IncidentProtocol.SCHEMA_VERSION,
                IncidentRequestType.CONFIRM,
                confirmEnvelope.getReportId(),
                0,
                null,
                null,
                confirmEnvelope);
    }

    public static IncidentRequest get(String reportId) {
        return new IncidentRequest(IncidentProtocol.SCHEMA_VERSION, IncidentRequestType.GET_REPORT, reportId, 0, null, null, null);
    }

    public static IncidentRequest list() {
        return new IncidentRequest(IncidentProtocol.SCHEMA_VERSION, IncidentRequestType.LIST_REPORTS, "", 0, null, null, null);
    }

    public static IncidentRequest head() {
        return new IncidentRequest(IncidentProtocol.SCHEMA_VERSION, IncidentRequestType.GET_LEDGER_HEAD, "", 0, null, null, null);
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public IncidentRequestType getType() {
        return type;
    }

    public String getReportId() {
        return reportId;
    }

    public int getRequiredConfirmations() {
        return requiredConfirmations;
    }

    public IncidentPayload getPayload() {
        return payload;
    }

    public SubmitEnvelope getSubmitEnvelope() {
        return submitEnvelope;
    }

    public ConfirmEnvelope getConfirmEnvelope() {
        return confirmEnvelope;
    }

    public byte[] toByteArray() {
        return CanonicalEncoder.encode(this);
    }

    @Override
    public void encode(CanonicalEncoder encoder) {
        encoder.writeInt(schemaVersion);
        encoder.writeInt(type.getCode());
        encoder.writeString(reportId);
        encoder.writeInt(requiredConfirmations);
        encoder.writeByte(payload == null ? 0 : 1);
        if (payload != null) {
            payload.encode(encoder);
        }
        encoder.writeByte(submitEnvelope == null ? 0 : 1);
        if (submitEnvelope != null) {
            submitEnvelope.encode(encoder);
        }
        encoder.writeByte(confirmEnvelope == null ? 0 : 1);
        if (confirmEnvelope != null) {
            confirmEnvelope.encode(encoder);
        }
    }

    public static IncidentRequest fromBytes(byte[] input) {
        CanonicalDecoder decoder = new CanonicalDecoder(input);
        IncidentRequest request = new IncidentRequest(
                decoder.readInt(),
                IncidentRequestType.fromCode(decoder.readInt()),
                decoder.readString(),
                decoder.readInt(),
                decoder.readByte() == 1 ? IncidentPayload.decode(decoder) : null,
                decoder.readByte() == 1 ? SubmitEnvelope.decode(decoder) : null,
                decoder.readByte() == 1 ? ConfirmEnvelope.decode(decoder) : null);
        decoder.requireFullyRead();
        return request;
    }
}
