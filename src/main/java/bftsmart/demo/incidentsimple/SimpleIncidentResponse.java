package bftsmart.demo.incidentsimple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class SimpleIncidentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private SimpleIncidentRecord record;
    private ArrayList<SimpleIncidentRecord> records;

    public static SimpleIncidentResponse success(String message, SimpleIncidentRecord record) {
        SimpleIncidentResponse response = new SimpleIncidentResponse();
        response.success = true;
        response.message = message;
        response.record = record;
        return response;
    }

    public static SimpleIncidentResponse success(String message, List<SimpleIncidentRecord> records) {
        SimpleIncidentResponse response = new SimpleIncidentResponse();
        response.success = true;
        response.message = message;
        response.records = new ArrayList<SimpleIncidentRecord>(records);
        return response;
    }

    public static SimpleIncidentResponse failure(String message) {
        SimpleIncidentResponse response = new SimpleIncidentResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public SimpleIncidentRecord getRecord() {
        return record;
    }

    public List<SimpleIncidentRecord> getRecords() {
        return records == null ? new ArrayList<SimpleIncidentRecord>() : new ArrayList<SimpleIncidentRecord>(records);
    }
}
