package bftsmart.demo.incident;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class IncidentRecord implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String UNSPECIFIED_HASH = "UNSPECIFIED";

    private final String reportId;
    private final String reporterId;
    private final double latitude;
    private final double longitude;
    private final String description;
    private final int validatorCount;
    private final int faultTolerance;
    private final int confirmationThreshold;
    private final TreeMap<String, String> witnessEvidenceHashes;
    private IncidentStatus status;
    private String lastMutationHash;

    public IncidentRecord(
            String reportId,
            String reporterId,
            double latitude,
            double longitude,
            String description,
            int validatorCount,
            int faultTolerance,
            String evidenceHash,
            int confirmationThreshold) {
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = normalizeText(description);
        this.validatorCount = validatorCount;
        this.faultTolerance = faultTolerance;
        this.confirmationThreshold = confirmationThreshold;
        this.witnessEvidenceHashes = new TreeMap<String, String>();
        this.status = IncidentStatus.PENDING;
        this.lastMutationHash = "";
        this.witnessEvidenceHashes.put(reporterId, normalizeHash(evidenceHash));
        refreshStatus();
    }

    public boolean addConfirmation(String shipId, String evidenceHash) {
        if (witnessEvidenceHashes.containsKey(shipId)) {
            return false;
        }
        witnessEvidenceHashes.put(shipId, normalizeHash(evidenceHash));
        refreshStatus();
        return true;
    }

    public String getReportId() {
        return reportId;
    }

    public String getReporterId() {
        return reporterId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getDescription() {
        return description;
    }

    public int getConfirmationThreshold() {
        return confirmationThreshold;
    }

    public int getValidatorCount() {
        return validatorCount;
    }

    public int getFaultTolerance() {
        return faultTolerance;
    }

    public int getConfirmationCount() {
        return witnessEvidenceHashes.size();
    }

    public Map<String, String> getWitnessEvidenceHashes() {
        return new TreeMap<String, String>(witnessEvidenceHashes);
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getLastMutationHash() {
        return lastMutationHash;
    }

    public void setLastMutationHash(String lastMutationHash) {
        this.lastMutationHash = lastMutationHash;
    }

    public String toDigestMaterial() {
        StringBuilder builder = new StringBuilder();
        builder.append("reportId=").append(reportId).append('\n');
        builder.append("reporterId=").append(reporterId).append('\n');
        builder.append("latitude=").append(formatCoordinate(latitude)).append('\n');
        builder.append("longitude=").append(formatCoordinate(longitude)).append('\n');
        builder.append("validatorCount=").append(validatorCount).append('\n');
        builder.append("faultTolerance=").append(faultTolerance).append('\n');
        builder.append("confirmationThreshold=").append(confirmationThreshold).append('\n');
        builder.append("status=").append(status.name()).append('\n');
        builder.append("description=").append(description.replace("\n", "\\n")).append('\n');
        for (Map.Entry<String, String> entry : witnessEvidenceHashes.entrySet()) {
            builder.append("witness=").append(entry.getKey()).append(':').append(entry.getValue()).append('\n');
        }
        return builder.toString();
    }

    private void refreshStatus() {
        status = (getConfirmationCount() >= confirmationThreshold) ? IncidentStatus.VERIFIED : IncidentStatus.PENDING;
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeHash(String value) {
        String normalized = normalizeText(value);
        return normalized.isEmpty() ? UNSPECIFIED_HASH : normalized;
    }

    public static String formatCoordinate(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }
}
