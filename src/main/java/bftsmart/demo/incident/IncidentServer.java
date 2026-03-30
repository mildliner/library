package bftsmart.demo.incident;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IncidentServer extends DefaultSingleRecoverable {

    private static final Logger LOGGER = Logger.getLogger(IncidentServer.class.getName());
    private static final String GENESIS_HASH = sha256("GENESIS");

    private TreeMap<String, IncidentRecord> reports = new TreeMap<String, IncidentRecord>();
    private long orderedRequests = 0L;
    private long committedEntries = 0L;
    private String ledgerHeadHash = GENESIS_HASH;

    public IncidentServer(int id) {
        new ServiceReplica(id, this, this);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Use: java IncidentServer <processId>");
            System.exit(-1);
        }
        new IncidentServer(Integer.parseInt(args[0]));
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        orderedRequests++;
        try {
            IncidentRequest request = IncidentMessageIO.fromBytes(command, IncidentRequest.class);
            IncidentResponse response;
            switch (request.getType()) {
                case SUBMIT_REPORT:
                    response = handleSubmit(request);
                    break;
                case CONFIRM_REPORT:
                    response = handleConfirm(request);
                    break;
                case GET_REPORT:
                    response = handleGet(request);
                    break;
                case LIST_REPORTS:
                    response = IncidentResponse.success(
                            "Listed " + reports.size() + " incident report(s).",
                            new ArrayList<IncidentRecord>(reports.values()),
                            ledgerHeadHash);
                    break;
                case GET_LEDGER_HEAD:
                    response = IncidentResponse.success("Current ledger head hash.", (IncidentRecord) null, ledgerHeadHash);
                    break;
                default:
                    response = IncidentResponse.failure("Unsupported ordered request: " + request.getType(), ledgerHeadHash);
                    break;
            }
            return IncidentMessageIO.toBytes(response);
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to process ordered request", e);
            return new byte[0];
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        try {
            IncidentRequest request = IncidentMessageIO.fromBytes(command, IncidentRequest.class);
            IncidentResponse response;
            switch (request.getType()) {
                case GET_REPORT:
                    response = handleGet(request);
                    break;
                case LIST_REPORTS:
                    response = IncidentResponse.success(
                            "Listed " + reports.size() + " incident report(s).",
                            new ArrayList<IncidentRecord>(reports.values()),
                            ledgerHeadHash);
                    break;
                case GET_LEDGER_HEAD:
                    response = IncidentResponse.success("Current ledger head hash.", (IncidentRecord) null, ledgerHeadHash);
                    break;
                default:
                    response = IncidentResponse.failure("Unsupported unordered request: " + request.getType(), ledgerHeadHash);
                    break;
            }
            return IncidentMessageIO.toBytes(response);
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to process unordered request", e);
            return new byte[0];
        }
    }

    @Override
    public void installSnapshot(byte[] state) {
        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(state);
             ObjectInput objectIn = new ObjectInputStream(byteIn)) {
            reports = castReports(objectIn.readObject());
            orderedRequests = objectIn.readLong();
            committedEntries = objectIn.readLong();
            ledgerHeadHash = (String) objectIn.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Error while installing snapshot", e);
        }
    }

    @Override
    public byte[] getSnapshot() {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutput objectOut = new ObjectOutputStream(byteOut)) {
            objectOut.writeObject(reports);
            objectOut.writeLong(orderedRequests);
            objectOut.writeLong(committedEntries);
            objectOut.writeObject(ledgerHeadHash);
            objectOut.flush();
            return byteOut.toByteArray();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while taking snapshot", e);
            return new byte[0];
        }
    }

    private IncidentResponse handleSubmit(IncidentRequest request) {
        String validationError = validateSubmit(request);
        if (validationError != null) {
            return IncidentResponse.failure(validationError, ledgerHeadHash);
        }

        if (reports.containsKey(request.getReportId())) {
            IncidentRecord existing = reports.get(request.getReportId());
            return IncidentResponse.failure(
                    "Report " + request.getReportId() + " already exists.",
                    existing,
                    ledgerHeadHash);
        }

        IncidentRecord record = new IncidentRecord(
                request.getReportId(),
                request.getShipId(),
                request.getLatitude(),
                request.getLongitude(),
                request.getDescription(),
                resolveValidatorCount(request),
                resolveFaultTolerance(request),
                request.getEvidenceHash(),
                resolveConfirmationThreshold(request));

        String nextHash = appendLedgerEntry("SUBMIT_REPORT", record);
        record.setLastMutationHash(nextHash);
        reports.put(record.getReportId(), record);

        LOGGER.info(String.format(
                "Committed report %s from %s. confirmations=%d/%d status=%s",
                record.getReportId(),
                record.getReporterId(),
                record.getConfirmationCount(),
                record.getConfirmationThreshold(),
                record.getStatus().name()));

        return IncidentResponse.success(
                "Report " + record.getReportId() + " registered.",
                record,
                ledgerHeadHash);
    }

    private IncidentResponse handleConfirm(IncidentRequest request) {
        String reportId = normalizeId(request.getReportId());
        String shipId = normalizeId(request.getShipId());
        if (reportId == null) {
            return IncidentResponse.failure("reportId must not be blank.", ledgerHeadHash);
        }
        if (shipId == null) {
            return IncidentResponse.failure("shipId must not be blank.", ledgerHeadHash);
        }

        IncidentRecord record = reports.get(reportId);
        if (record == null) {
            return IncidentResponse.failure("Report " + reportId + " does not exist.", ledgerHeadHash);
        }

        if (!record.addConfirmation(shipId, request.getEvidenceHash())) {
            return IncidentResponse.success(
                    "Ship " + shipId + " had already confirmed report " + reportId + ".",
                    record,
                    ledgerHeadHash);
        }

        String nextHash = appendLedgerEntry("CONFIRM_REPORT", record);
        record.setLastMutationHash(nextHash);

        LOGGER.info(String.format(
                "Confirmed report %s by %s. confirmations=%d/%d status=%s",
                record.getReportId(),
                shipId,
                record.getConfirmationCount(),
                record.getConfirmationThreshold(),
                record.getStatus().name()));

        String message = "Confirmation from ship " + shipId + " recorded for report " + reportId + ".";
        if (record.getStatus() == IncidentStatus.VERIFIED) {
            message = "Report " + reportId + " reached the verification threshold.";
        }
        return IncidentResponse.success(message, record, ledgerHeadHash);
    }

    private IncidentResponse handleGet(IncidentRequest request) {
        String reportId = normalizeId(request.getReportId());
        if (reportId == null) {
            return IncidentResponse.failure("reportId must not be blank.", ledgerHeadHash);
        }
        IncidentRecord record = reports.get(reportId);
        if (record == null) {
            return IncidentResponse.failure("Report " + reportId + " does not exist.", ledgerHeadHash);
        }
        return IncidentResponse.success("Fetched report " + reportId + ".", record, ledgerHeadHash);
    }

    private String appendLedgerEntry(String operation, IncidentRecord record) {
        committedEntries++;
        StringBuilder payload = new StringBuilder();
        payload.append("entry=").append(committedEntries).append('\n');
        payload.append("operation=").append(operation).append('\n');
        payload.append("previous=").append(ledgerHeadHash).append('\n');
        payload.append(record.toDigestMaterial());
        ledgerHeadHash = sha256(payload.toString());
        return ledgerHeadHash;
    }

    private static String validateSubmit(IncidentRequest request) {
        if (normalizeId(request.getReportId()) == null) {
            return "reportId must not be blank.";
        }
        if (normalizeId(request.getShipId()) == null) {
            return "shipId must not be blank.";
        }
        if (request.getFaultTolerance() > 0) {
            if (request.getFaultTolerance() < 1) {
                return "faultTolerance must be at least 1.";
            }
        } else if (request.getConfirmationThreshold() < 1) {
            return "confirmationThreshold must be at least 1.";
        }
        if (Double.isNaN(request.getLatitude()) || request.getLatitude() < -90.0d || request.getLatitude() > 90.0d) {
            return "latitude must be between -90 and 90.";
        }
        if (Double.isNaN(request.getLongitude()) || request.getLongitude() < -180.0d || request.getLongitude() > 180.0d) {
            return "longitude must be between -180 and 180.";
        }
        return null;
    }

    private static String normalizeId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static int resolveValidatorCount(IncidentRequest request) {
        if (request.getFaultTolerance() > 0) {
            return PbftMath.validatorCountForFaultTolerance(request.getFaultTolerance());
        }
        return request.getConfirmationThreshold();
    }

    private static int resolveFaultTolerance(IncidentRequest request) {
        return request.getFaultTolerance();
    }

    private static int resolveConfirmationThreshold(IncidentRequest request) {
        if (request.getFaultTolerance() > 0) {
            return PbftMath.quorumForFaultTolerance(request.getFaultTolerance());
        }
        return request.getConfirmationThreshold();
    }

    @SuppressWarnings("unchecked")
    private static TreeMap<String, IncidentRecord> castReports(Object value) {
        return (TreeMap<String, IncidentRecord>) value;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                hex.append(String.format("%02x", current & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
