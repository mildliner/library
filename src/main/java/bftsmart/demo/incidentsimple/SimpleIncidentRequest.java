package bftsmart.demo.incidentsimple;

import java.io.Serializable;

public final class SimpleIncidentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private SimpleIncidentRequestType type;
    private String incidentId;
    private String shipId;
    private String description;
    private int confirmationThreshold;
    private long createdAt;

    public static SimpleIncidentRequest submit(String incidentId, String reporterShipId, String description, int confirmationThreshold) {
        SimpleIncidentRequest request = new SimpleIncidentRequest();
        request.type = SimpleIncidentRequestType.SUBMIT_INCIDENT;
        request.incidentId = incidentId;
        request.shipId = reporterShipId;
        request.description = description;
        request.confirmationThreshold = confirmationThreshold;
        request.createdAt = System.currentTimeMillis();
        return request;
    }

    public static SimpleIncidentRequest confirm(String incidentId, String confirmerShipId) {
        SimpleIncidentRequest request = new SimpleIncidentRequest();
        request.type = SimpleIncidentRequestType.CONFIRM_INCIDENT;
        request.incidentId = incidentId;
        request.shipId = confirmerShipId;
        return request;
    }

    public static SimpleIncidentRequest get(String incidentId) {
        SimpleIncidentRequest request = new SimpleIncidentRequest();
        request.type = SimpleIncidentRequestType.GET_INCIDENT;
        request.incidentId = incidentId;
        return request;
    }

    public static SimpleIncidentRequest list() {
        SimpleIncidentRequest request = new SimpleIncidentRequest();
        request.type = SimpleIncidentRequestType.LIST_INCIDENTS;
        return request;
    }

    public SimpleIncidentRequestType getType() {
        return type;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getShipId() {
        return shipId;
    }

    public String getDescription() {
        return description;
    }

    public int getConfirmationThreshold() {
        return confirmationThreshold;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
