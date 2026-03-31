package bftsmart.demo.incident;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TreeMap;

public final class IncidentRecord implements CanonicalWritable {

    private final IncidentPayload payload;
    private final byte[] reportHash;
    private final SubmitEnvelope submit;
    private final TreeMap<String, ConfirmationRecord> confirmationsByShipId;
    private IncidentStatus status;
    private int validConfirmationCount;
    private byte[] confirmationRoot;
    private byte[] stateDigest;
    private final long createdAtMs;
    private long updatedAtMs;

    public IncidentRecord(IncidentPayload payload, byte[] reportHash, SubmitEnvelope submit, long createdAtMs, long updatedAtMs) {
        this.payload = payload;
        this.reportHash = reportHash == null ? new byte[0] : reportHash.clone();
        this.submit = submit;
        this.confirmationsByShipId = new TreeMap<String, ConfirmationRecord>();
        this.status = IncidentStatus.PENDING;
        this.validConfirmationCount = 0;
        this.confirmationRoot = CryptoUtils.zeroDigest();
        this.stateDigest = CryptoUtils.zeroDigest();
        this.createdAtMs = createdAtMs;
        this.updatedAtMs = updatedAtMs;
        refreshDerivedDigests();
    }

    private IncidentRecord(
            IncidentPayload payload,
            byte[] reportHash,
            SubmitEnvelope submit,
            TreeMap<String, ConfirmationRecord> confirmationsByShipId,
            IncidentStatus status,
            int validConfirmationCount,
            byte[] confirmationRoot,
            byte[] stateDigest,
            long createdAtMs,
            long updatedAtMs) {
        this.payload = payload;
        this.reportHash = reportHash == null ? new byte[0] : reportHash.clone();
        this.submit = submit;
        this.confirmationsByShipId = confirmationsByShipId;
        this.status = status;
        this.validConfirmationCount = validConfirmationCount;
        this.confirmationRoot = confirmationRoot == null ? new byte[0] : confirmationRoot.clone();
        this.stateDigest = stateDigest == null ? new byte[0] : stateDigest.clone();
        this.createdAtMs = createdAtMs;
        this.updatedAtMs = updatedAtMs;
    }

    public IncidentPayload getPayload() {
        return payload;
    }

    public String getReportId() {
        return payload.getReportId();
    }

    public String getReporterShipId() {
        return payload.getReporterShipId();
    }

    public byte[] getReportHash() {
        return reportHash.clone();
    }

    public SubmitEnvelope getSubmit() {
        return submit;
    }

    public Map<String, ConfirmationRecord> getConfirmationsByShipId() {
        return new TreeMap<String, ConfirmationRecord>(confirmationsByShipId);
    }

    public ConfirmationRecord getConfirmation(String shipId) {
        return confirmationsByShipId.get(shipId);
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public boolean isTerminal() {
        return status == IncidentStatus.VERIFIED || status == IncidentStatus.EXPIRED;
    }

    public int getValidConfirmationCount() {
        return validConfirmationCount;
    }

    public byte[] getConfirmationRoot() {
        return confirmationRoot.clone();
    }

    public byte[] getStateDigest() {
        return stateDigest.clone();
    }

    public long getCreatedAtMs() {
        return createdAtMs;
    }

    public long getUpdatedAtMs() {
        return updatedAtMs;
    }

    public long getExpiresAtMs() {
        return submit.getExpiresAtMs();
    }

    public void addConfirmation(ConfirmationRecord confirmationRecord, long updatedAtMs) {
        confirmationsByShipId.put(confirmationRecord.getConfirmerShipId(), confirmationRecord);
        this.updatedAtMs = updatedAtMs;
        refreshDerivedDigests();
    }

    public void setStatus(IncidentStatus status, long updatedAtMs) {
        this.status = status;
        this.updatedAtMs = updatedAtMs;
        refreshDerivedDigests();
    }

    public void refreshDerivedDigests() {
        validConfirmationCount = confirmationsByShipId.size();
        confirmationRoot = MerkleTree.computeConfirmationRoot(confirmationsByShipId.values());
        stateDigest = computeStateDigest(reportHash, submit, confirmationRoot, status, validConfirmationCount);
    }

    public static byte[] computeStateDigest(
            byte[] reportHash,
            SubmitEnvelope submit,
            byte[] confirmationRoot,
            IncidentStatus status,
            int validConfirmationCount) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(0x20);
        output.write(reportHash, 0, reportHash.length);
        byte[] submitSignatureDigest = CryptoUtils.sha256(submit.getSignatureBytes());
        output.write(submitSignatureDigest, 0, submitSignatureDigest.length);
        output.write(confirmationRoot, 0, confirmationRoot.length);
        writeInt(output, status.getCode());
        writeInt(output, validConfirmationCount);
        return CryptoUtils.sha256(output.toByteArray());
    }

    private static void writeInt(ByteArrayOutputStream output, int value) {
        output.write((value >>> 24) & 0xff);
        output.write((value >>> 16) & 0xff);
        output.write((value >>> 8) & 0xff);
        output.write(value & 0xff);
    }

    @Override
    public void encode(CanonicalEncoder encoder) {
        payload.encode(encoder);
        encoder.writeByteArray(reportHash);
        submit.encode(encoder);
        encoder.writeInt(confirmationsByShipId.size());
        for (ConfirmationRecord record : confirmationsByShipId.values()) {
            record.encode(encoder);
        }
        encoder.writeInt(status.getCode());
        encoder.writeInt(validConfirmationCount);
        encoder.writeByteArray(confirmationRoot);
        encoder.writeByteArray(stateDigest);
        encoder.writeLong(createdAtMs);
        encoder.writeLong(updatedAtMs);
    }

    public static IncidentRecord decode(CanonicalDecoder decoder) {
        IncidentPayload payload = IncidentPayload.decode(decoder);
        byte[] reportHash = decoder.readByteArray();
        SubmitEnvelope submit = SubmitEnvelope.decode(decoder);
        int count = decoder.readInt();
        TreeMap<String, ConfirmationRecord> confirmations = new TreeMap<String, ConfirmationRecord>();
        for (int i = 0; i < count; i++) {
            ConfirmationRecord record = ConfirmationRecord.decode(decoder);
            confirmations.put(record.getConfirmerShipId(), record);
        }
        return new IncidentRecord(
                payload,
                reportHash,
                submit,
                confirmations,
                IncidentStatus.fromCode(decoder.readInt()),
                decoder.readInt(),
                decoder.readByteArray(),
                decoder.readByteArray(),
                decoder.readLong(),
                decoder.readLong());
    }
}
