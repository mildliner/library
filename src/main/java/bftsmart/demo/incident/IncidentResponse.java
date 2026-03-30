package bftsmart.demo.incident;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IncidentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private IncidentRecord record;
    private ArrayList<IncidentRecord> records;
    private String ledgerHeadHash;

    public static IncidentResponse success(String message, IncidentRecord record, String ledgerHeadHash) {
        IncidentResponse response = new IncidentResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setRecord(record);
        response.setLedgerHeadHash(ledgerHeadHash);
        return response;
    }

    public static IncidentResponse success(String message, List<IncidentRecord> records, String ledgerHeadHash) {
        IncidentResponse response = new IncidentResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setRecords(records);
        response.setLedgerHeadHash(ledgerHeadHash);
        return response;
    }

    public static IncidentResponse failure(String message, IncidentRecord record, String ledgerHeadHash) {
        IncidentResponse response = new IncidentResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setRecord(record);
        response.setLedgerHeadHash(ledgerHeadHash);
        return response;
    }

    public static IncidentResponse failure(String message, String ledgerHeadHash) {
        return failure(message, null, ledgerHeadHash);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public IncidentRecord getRecord() {
        return record;
    }

    public void setRecord(IncidentRecord record) {
        this.record = record;
    }

    public List<IncidentRecord> getRecords() {
        return records;
    }

    public void setRecords(List<IncidentRecord> records) {
        this.records = new ArrayList<IncidentRecord>(records);
    }

    public String getLedgerHeadHash() {
        return ledgerHeadHash;
    }

    public void setLedgerHeadHash(String ledgerHeadHash) {
        this.ledgerHeadHash = ledgerHeadHash;
    }
}
