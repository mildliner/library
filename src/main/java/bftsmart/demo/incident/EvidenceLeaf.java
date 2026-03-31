package bftsmart.demo.incident;

public final class EvidenceLeaf implements CanonicalWritable {

    private final int sourceTypeCode;
    private final String sensorId;
    private final long sequenceNo;
    private final byte[] evidenceBytes;

    public EvidenceLeaf(int sourceTypeCode, String sensorId, long sequenceNo, byte[] evidenceBytes) {
        this.sourceTypeCode = sourceTypeCode;
        this.sensorId = sensorId == null ? "" : sensorId;
        this.sequenceNo = sequenceNo;
        this.evidenceBytes = evidenceBytes == null ? new byte[0] : evidenceBytes.clone();
    }

    public int getSourceTypeCode() {
        return sourceTypeCode;
    }

    public String getSensorId() {
        return sensorId;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public byte[] getEvidenceBytes() {
        return evidenceBytes.clone();
    }

    public byte[] canonicalLeafBytes() {
        return CanonicalEncoder.encode(this);
    }

    @Override
    public void encode(CanonicalEncoder encoder) {
        encoder.writeInt(sourceTypeCode);
        encoder.writeString(sensorId);
        encoder.writeLong(sequenceNo);
        encoder.writeByteArray(evidenceBytes);
    }

    public static EvidenceLeaf decode(CanonicalDecoder decoder) {
        return new EvidenceLeaf(
                decoder.readInt(),
                decoder.readString(),
                decoder.readLong(),
                decoder.readByteArray());
    }
}
