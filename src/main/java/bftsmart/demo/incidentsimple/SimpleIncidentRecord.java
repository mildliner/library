package bftsmart.demo.incidentsimple;

import java.io.Serializable;
import java.util.TreeSet;

public final class SimpleIncidentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String incidentId;
    private final String reporterShipId;
    private final String description;
    private final long createdAt;
    private final TreeSet<String> confirmShips;
    private SimpleIncidentStatus status;

    public SimpleIncidentRecord(String incidentId, String reporterShipId, String description, long createdAt) {
        this.incidentId = incidentId;
        this.reporterShipId = reporterShipId;
        this.description = description == null ? "" : description;
        this.createdAt = createdAt;
        this.confirmShips = new TreeSet<String>();
        this.status = SimpleIncidentStatus.PENDING;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getReporterShipId() {
        return reporterShipId;
    }

    public String getDescription() {
        return description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public SimpleIncidentStatus getStatus() {
        return status;
    }

    public int getConfirmationCount() {
        return confirmShips.size();
    }

    public TreeSet<String> getConfirmShips() {
        return new TreeSet<String>(confirmShips);
    }

    public boolean addConfirmer(String shipId, int threshold) {
        if (!confirmShips.add(shipId)) {
            return false;
        }
        if (confirmShips.size() >= threshold) {
            status = SimpleIncidentStatus.VERIFIED;
        }
        return true;
    }
}
