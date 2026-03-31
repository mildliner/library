package bftsmart.demo.incident;

public final class ConfirmationRecord implements CanonicalWritable {

    private final ConfirmEnvelope envelope;
    private final byte[] confirmPayloadDigest;
    private final long acceptedAtMs;

    public ConfirmationRecord(ConfirmEnvelope envelope, byte[] confirmPayloadDigest, long acceptedAtMs) {
        this.envelope = envelope;
        this.confirmPayloadDigest = confirmPayloadDigest == null ? new byte[0] : confirmPayloadDigest.clone();
        this.acceptedAtMs = acceptedAtMs;
    }

    public static byte[] computePayloadDigest(ConfirmEnvelope envelope) {
        CanonicalEncoder encoder = new CanonicalEncoder();
        encoder.writeInt(envelope.getSchemaVersion());
        encoder.writeString(envelope.getAction());
        encoder.writeByteArray(envelope.getReportHash());
        encoder.writeString(envelope.getReportId());
        encoder.writeString(envelope.getConfirmerShipId());
        encoder.writeByteArray(envelope.getWitnessEvidenceRoot());
        encoder.writeInt(envelope.getDecisionCode());
        return CryptoUtils.sha256(encoder.toByteArray());
    }

    public ConfirmEnvelope getEnvelope() {
        return envelope;
    }

    public String getConfirmerShipId() {
        return envelope.getConfirmerShipId();
    }

    public byte[] getWitnessEvidenceRoot() {
        return envelope.getWitnessEvidenceRoot();
    }

    public int getDecisionCode() {
        return envelope.getDecisionCode();
    }

    public byte[] getConfirmPayloadDigest() {
        return confirmPayloadDigest.clone();
    }

    public long getAcceptedAtMs() {
        return acceptedAtMs;
    }

    public byte[] toMerkleLeafBytes() {
        CanonicalEncoder encoder = new CanonicalEncoder();
        encoder.writeString(getConfirmerShipId());
        encoder.writeByteArray(getWitnessEvidenceRoot());
        encoder.writeInt(getDecisionCode());
        encoder.writeByteArray(confirmPayloadDigest);
        return encoder.toByteArray();
    }

    @Override
    public void encode(CanonicalEncoder encoder) {
        envelope.encode(encoder);
        encoder.writeByteArray(confirmPayloadDigest);
        encoder.writeLong(acceptedAtMs);
    }

    public static ConfirmationRecord decode(CanonicalDecoder decoder) {
        return new ConfirmationRecord(
                ConfirmEnvelope.decode(decoder),
                decoder.readByteArray(),
                decoder.readLong());
    }
}
