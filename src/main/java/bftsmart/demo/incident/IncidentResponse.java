package bftsmart.demo.incident;

import java.util.ArrayList;
import java.util.List;

public final class IncidentResponse implements CanonicalWritable {

    private final int schemaVersion;
    private final boolean success;
    private final String message;
    private final IncidentRecord record;
    private final ArrayList<IncidentRecord> records;
    private final byte[] headDigest;

    private IncidentResponse(
            int schemaVersion,
            boolean success,
            String message,
            IncidentRecord record,
            List<IncidentRecord> records,
            byte[] headDigest) {
        this.schemaVersion = schemaVersion;
        this.success = success;
        this.message = message == null ? "" : message;
        this.record = record;
        this.records = new ArrayList<IncidentRecord>(records == null ? new ArrayList<IncidentRecord>() : records);
        this.headDigest = headDigest == null ? new byte[0] : headDigest.clone();
    }

    public static IncidentResponse success(String message, IncidentRecord record, byte[] headDigest) {
        return new IncidentResponse(IncidentProtocol.SCHEMA_VERSION, true, message, record, null, headDigest);
    }

    public static IncidentResponse success(String message, List<IncidentRecord> records, byte[] headDigest) {
        return new IncidentResponse(IncidentProtocol.SCHEMA_VERSION, true, message, null, records, headDigest);
    }

    public static IncidentResponse failure(String message, IncidentRecord record, byte[] headDigest) {
        return new IncidentResponse(IncidentProtocol.SCHEMA_VERSION, false, message, record, null, headDigest);
    }

    public static IncidentResponse failure(String message, byte[] headDigest) {
        return failure(message, null, headDigest);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public IncidentRecord getRecord() {
        return record;
    }

    public List<IncidentRecord> getRecords() {
        return new ArrayList<IncidentRecord>(records);
    }

    public byte[] getHeadDigest() {
        return headDigest.clone();
    }

    public byte[] toByteArray() {
        return CanonicalEncoder.encode(this);
    }

    @Override
    public void encode(CanonicalEncoder encoder) {
        encoder.writeInt(schemaVersion);
        encoder.writeByte(success ? 1 : 0);
        encoder.writeString(message);
        encoder.writeByte(record == null ? 0 : 1);
        if (record != null) {
            record.encode(encoder);
        }
        encoder.writeInt(records.size());
        for (IncidentRecord value : records) {
            value.encode(encoder);
        }
        encoder.writeByteArray(headDigest);
    }

    public static IncidentResponse fromBytes(byte[] input) {
        CanonicalDecoder decoder = new CanonicalDecoder(input);
        int schemaVersion = decoder.readInt();
        boolean success = decoder.readByte() == 1;
        String message = decoder.readString();
        IncidentRecord record = decoder.readByte() == 1 ? IncidentRecord.decode(decoder) : null;
        int size = decoder.readInt();
        ArrayList<IncidentRecord> records = new ArrayList<IncidentRecord>(size);
        for (int i = 0; i < size; i++) {
            records.add(IncidentRecord.decode(decoder));
        }
        byte[] headDigest = decoder.readByteArray();
        decoder.requireFullyRead();
        return new IncidentResponse(schemaVersion, success, message, record, records, headDigest);
    }
}
