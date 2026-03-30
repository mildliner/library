package bftsmart.demo.incident;

import java.io.Serializable;

public class IncidentRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private IncidentRequestType type;
    private String reportId;
    private String shipId;
    private double latitude;
    private double longitude;
    private String description;
    private String evidenceHash;
    private int confirmationThreshold;
    private int faultTolerance;

    public IncidentRequestType getType() {
        return type;
    }

    public void setType(IncidentRequestType type) {
        this.type = type;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getShipId() {
        return shipId;
    }

    public void setShipId(String shipId) {
        this.shipId = shipId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidenceHash() {
        return evidenceHash;
    }

    public void setEvidenceHash(String evidenceHash) {
        this.evidenceHash = evidenceHash;
    }

    public int getConfirmationThreshold() {
        return confirmationThreshold;
    }

    public void setConfirmationThreshold(int confirmationThreshold) {
        this.confirmationThreshold = confirmationThreshold;
    }

    public int getFaultTolerance() {
        return faultTolerance;
    }

    public void setFaultTolerance(int faultTolerance) {
        this.faultTolerance = faultTolerance;
    }
}
