package bftsmart.demo.incident;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class IncidentServer extends DefaultSingleRecoverable {

    private static final Logger LOGGER = Logger.getLogger(IncidentServer.class.getName());

    private final ShipMembership membership;
    private final long allowedClockSkewMs;

    private TreeMap<String, IncidentRecord> reports = new TreeMap<String, IncidentRecord>();
    private TreeMap<String, Integer> requiredConfirmationsByReportId = new TreeMap<String, Integer>();
    private TreeMap<String, IdempotencyRecord> processedRequests = new TreeMap<String, IdempotencyRecord>();

    public IncidentServer(int processId) {
        this(processId, ShipMembership.defaultMembership(), IncidentProtocol.DEFAULT_ALLOWED_CLOCK_SKEW_MS);
    }

    IncidentServer(int processId, ShipMembership membership, long allowedClockSkewMs) {
        this.membership = membership;
        this.allowedClockSkewMs = allowedClockSkewMs;
        new ServiceReplica(processId, this, this);
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
        long referenceTimeMs = resolveReferenceTime(msgCtx);
        try {
            IncidentRequest request = IncidentRequest.fromBytes(command);
            expirePendingReports(referenceTimeMs);
            IncidentResponse response = handleOrdered(request, referenceTimeMs);
            return response.toByteArray();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to process ordered request", e);
            return IncidentResponse.failure("Failed to process ordered request: " + e.getMessage(), computeLedgerHeadDigest())
                    .toByteArray();
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        try {
            IncidentRequest request = IncidentRequest.fromBytes(command);
            IncidentResponse response;
            switch (request.getType()) {
                case GET_REPORT:
                    response = handleGet(request.getReportId());
                    break;
                case LIST_REPORTS:
                    response = IncidentResponse.success(
                            "Listed " + reports.size() + " incident report(s).",
                            new ArrayList<IncidentRecord>(reports.values()),
                            computeLedgerHeadDigest());
                    break;
                case GET_LEDGER_HEAD:
                    response = IncidentResponse.success("Current incident ledger head digest.", (IncidentRecord) null, computeLedgerHeadDigest());
                    break;
                default:
                    response = IncidentResponse.failure("Unordered access is only allowed for get/list/head.", computeLedgerHeadDigest());
                    break;
            }
            return response.toByteArray();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to process unordered request", e);
            return IncidentResponse.failure("Failed to process unordered request: " + e.getMessage(), computeLedgerHeadDigest())
                    .toByteArray();
        }
    }

    @Override
    public void installSnapshot(byte[] state) {
        try {
            CanonicalDecoder decoder = new CanonicalDecoder(state);
            int schemaVersion = decoder.readInt();
            if (schemaVersion != IncidentProtocol.SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported snapshot schema version: " + schemaVersion);
            }

            TreeMap<String, IncidentRecord> nextReports = new TreeMap<String, IncidentRecord>();
            TreeMap<String, Integer> nextRequired = new TreeMap<String, Integer>();
            int reportCount = decoder.readInt();
            for (int i = 0; i < reportCount; i++) {
                IncidentRecord record = IncidentRecord.decode(decoder);
                nextReports.put(record.getReportId(), record);
                nextRequired.put(record.getReportId(), decoder.readInt());
            }

            TreeMap<String, IdempotencyRecord> nextIdempotency = new TreeMap<String, IdempotencyRecord>();
            int idempotencyCount = decoder.readInt();
            for (int i = 0; i < idempotencyCount; i++) {
                IdempotencyRecord record = IdempotencyRecord.decode(decoder);
                nextIdempotency.put(record.getRequestId(), record);
            }
            decoder.requireFullyRead();

            reports = nextReports;
            requiredConfirmationsByReportId = nextRequired;
            processedRequests = nextIdempotency;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while installing snapshot", e);
        }
    }

    @Override
    public byte[] getSnapshot() {
        try {
            CanonicalEncoder encoder = new CanonicalEncoder();
            encoder.writeInt(IncidentProtocol.SCHEMA_VERSION);
            encoder.writeInt(reports.size());
            for (Map.Entry<String, IncidentRecord> entry : reports.entrySet()) {
                entry.getValue().encode(encoder);
                Integer required = requiredConfirmationsByReportId.get(entry.getKey());
                encoder.writeInt(required == null ? 0 : required.intValue());
            }
            encoder.writeInt(processedRequests.size());
            for (IdempotencyRecord record : processedRequests.values()) {
                record.encode(encoder);
            }
            return encoder.toByteArray();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while taking snapshot", e);
            return new byte[0];
        }
    }

    private IncidentResponse handleOrdered(IncidentRequest request, long referenceTimeMs) {
        switch (request.getType()) {
            case SUBMIT:
                return handleSubmit(request, referenceTimeMs);
            case CONFIRM:
                return handleConfirm(request, referenceTimeMs);
            case GET_REPORT:
                return handleGet(request.getReportId());
            case LIST_REPORTS:
                return IncidentResponse.success(
                        "Listed " + reports.size() + " incident report(s).",
                        new ArrayList<IncidentRecord>(reports.values()),
                        computeLedgerHeadDigest());
            case GET_LEDGER_HEAD:
                return IncidentResponse.success("Current incident ledger head digest.", (IncidentRecord) null, computeLedgerHeadDigest());
            default:
                return IncidentResponse.failure("Unsupported request type: " + request.getType(), computeLedgerHeadDigest());
        }
    }

    private IncidentResponse handleSubmit(IncidentRequest request, long referenceTimeMs) {
        IncidentPayload payload = request.getPayload();
        SubmitEnvelope submit = request.getSubmitEnvelope();
        if (payload == null || submit == null) {
            return failure("Submit requires both payload and submit envelope.", null);
        }
        if (request.getRequiredConfirmations() < 1) {
            return failure("requiredConfirmations must be at least 1.", null);
        }
        if (payload.getSchemaVersion() != IncidentProtocol.SCHEMA_VERSION
                || submit.getSchemaVersion() != IncidentProtocol.SCHEMA_VERSION
                || request.getSchemaVersion() != IncidentProtocol.SCHEMA_VERSION) {
            return failure("Unsupported schemaVersion.", null);
        }
        if (!IncidentProtocol.ACTION_SUBMIT.equals(submit.getAction())) {
            return failure("Submit envelope action must be SUBMIT.", null);
        }
        if (!IncidentProtocol.SIGNATURE_ALGORITHM.equals(submit.getSignatureAlgorithm())) {
            return failure("Submit envelope signature algorithm must be SHA256withECDSA.", null);
        }
        if (isBlank(payload.getReportId()) || isBlank(payload.getReporterShipId()) || isBlank(payload.getReporterKeyId())) {
            return failure("reportId, reporterShipId, and reporterKeyId must not be blank.", null);
        }
        if (!payload.getReportId().equals(submit.getReportId()) || !payload.getReporterShipId().equals(submit.getReporterShipId())) {
            return failure("Submit payload and envelope do not match reportId/reporterShipId.", null);
        }
        if (!membership.isMember(payload.getReporterShipId())) {
            return failure("Reporter shipId is not a recognized member.", null);
        }
        if (!membership.shipOwnsKey(payload.getReporterShipId(), payload.getReporterKeyId())) {
            return failure("reporterKeyId does not belong to reporterShipId.", null);
        }
        if (isBlank(submit.getRequestId())) {
            return failure("Submit requestId must not be blank.", null);
        }
        String clockError = validateEnvelopeClock(submit.getIssuedAtMs(), submit.getExpiresAtMs(), referenceTimeMs);
        if (clockError != null) {
            return failure(clockError, null);
        }

        byte[] computedReportHash = payload.reportHash();
        if (!CryptoUtils.equals(computedReportHash, submit.getReportHash())) {
            return failure("reportHash does not match canonical IncidentPayload.", null);
        }

        ShipIdentity identity = membership.getByShipId(payload.getReporterShipId());
        if (!CryptoUtils.verify(identity.getPublicKey(), submit.toBeSignedBytes(), submit.getSignatureBytes())) {
            return failure("Submit signature mismatch.", null);
        }

        byte[] semanticDigest = buildSubmitSemanticDigest(payload, submit, request.getRequiredConfirmations());
        IdempotencyRecord idempotencyRecord = processedRequests.get(submit.getRequestId());
        if (idempotencyRecord != null) {
            if (idempotencyRecord.getActionCode() != IncidentRequestType.SUBMIT.getCode()
                    || !CryptoUtils.equals(idempotencyRecord.getSemanticDigest(), semanticDigest)) {
                return failure("Submit requestId was already used for a different request.", null);
            }
            IncidentRecord existing = reports.get(payload.getReportId());
            if (existing != null) {
                return success("Submit request was already processed.", existing);
            }
        }

        IncidentRecord existing = reports.get(payload.getReportId());
        if (existing != null) {
            if (!CryptoUtils.equals(existing.getReportHash(), computedReportHash)) {
                return failure("The same reportId cannot be reused with a different reportHash.", existing);
            }
            processedRequests.put(submit.getRequestId(), new IdempotencyRecord(
                    submit.getRequestId(),
                    IncidentRequestType.SUBMIT.getCode(),
                    semanticDigest,
                    payload.getReportId(),
                    payload.getReporterShipId()));
            return success("Report " + payload.getReportId() + " already exists with the same reportHash.", existing);
        }

        IncidentRecord record = new IncidentRecord(payload, computedReportHash, submit, referenceTimeMs, referenceTimeMs);
        reports.put(payload.getReportId(), record);
        requiredConfirmationsByReportId.put(payload.getReportId(), Integer.valueOf(request.getRequiredConfirmations()));
        processedRequests.put(submit.getRequestId(), new IdempotencyRecord(
                submit.getRequestId(),
                IncidentRequestType.SUBMIT.getCode(),
                semanticDigest,
                payload.getReportId(),
                payload.getReporterShipId()));

        LOGGER.info(String.format(
                "Committed SUBMIT for %s by %s, requiredConfirmations=%d, reportHash=%s",
                payload.getReportId(),
                payload.getReporterShipId(),
                request.getRequiredConfirmations(),
                HexUtils.toHex(record.getReportHash())));

        return success(
                "Report " + payload.getReportId() + " registered. Confirmations: 0/" + request.getRequiredConfirmations(),
                record);
    }

    private IncidentResponse handleConfirm(IncidentRequest request, long referenceTimeMs) {
        ConfirmEnvelope confirm = request.getConfirmEnvelope();
        if (confirm == null) {
            return failure("Confirm requires a confirm envelope.", null);
        }
        if (confirm.getSchemaVersion() != IncidentProtocol.SCHEMA_VERSION
                || request.getSchemaVersion() != IncidentProtocol.SCHEMA_VERSION) {
            return failure("Unsupported schemaVersion.", null);
        }
        if (!IncidentProtocol.ACTION_CONFIRM.equals(confirm.getAction())) {
            return failure("Confirm envelope action must be CONFIRM.", null);
        }
        if (!IncidentProtocol.SIGNATURE_ALGORITHM.equals(confirm.getSignatureAlgorithm())) {
            return failure("Confirm envelope signature algorithm must be SHA256withECDSA.", null);
        }
        if (isBlank(confirm.getReportId()) || isBlank(confirm.getConfirmerShipId()) || isBlank(confirm.getRequestId())) {
            return failure("reportId, confirmerShipId, and requestId must not be blank.", null);
        }
        if (!membership.isMember(confirm.getConfirmerShipId())) {
            return failure("Confirmer shipId is not a recognized member.", null);
        }
        String clockError = validateEnvelopeClock(confirm.getIssuedAtMs(), confirm.getExpiresAtMs(), referenceTimeMs);
        if (clockError != null) {
            return failure(clockError, null);
        }

        IncidentRecord record = reports.get(confirm.getReportId());
        if (record == null) {
            return failure("Report " + confirm.getReportId() + " does not exist.", null);
        }
        if (!CryptoUtils.equals(record.getReportHash(), confirm.getReportHash())) {
            return failure("Confirm reportHash does not match the submitted reportHash.", record);
        }
        if (record.getStatus() == IncidentStatus.EXPIRED) {
            return failure("Report " + confirm.getReportId() + " is already EXPIRED.", record);
        }
        if (record.getReporterShipId().equals(confirm.getConfirmerShipId())) {
            return failure("The submitter does not count as a confirmer.", record);
        }

        ShipIdentity identity = membership.getByShipId(confirm.getConfirmerShipId());
        if (!CryptoUtils.verify(identity.getPublicKey(), confirm.toBeSignedBytes(), confirm.getSignatureBytes())) {
            return failure("Confirm signature mismatch.", record);
        }

        byte[] semanticDigest = buildConfirmSemanticDigest(confirm);
        IdempotencyRecord idempotencyRecord = processedRequests.get(confirm.getRequestId());
        if (idempotencyRecord != null) {
            if (idempotencyRecord.getActionCode() != IncidentRequestType.CONFIRM.getCode()
                    || !CryptoUtils.equals(idempotencyRecord.getSemanticDigest(), semanticDigest)) {
                return failure("Confirm requestId was already used for a different request.", record);
            }
            return success("Confirm request was already processed.", record);
        }

        ConfirmationRecord existingConfirmation = record.getConfirmation(confirm.getConfirmerShipId());
        byte[] confirmPayloadDigest = ConfirmationRecord.computePayloadDigest(confirm);
        if (existingConfirmation != null) {
            if (!CryptoUtils.equals(existingConfirmation.getConfirmPayloadDigest(), confirmPayloadDigest)) {
                return failure("duplicate confirmerShipId with different confirm payload is not allowed.", record);
            }
            processedRequests.put(confirm.getRequestId(), new IdempotencyRecord(
                    confirm.getRequestId(),
                    IncidentRequestType.CONFIRM.getCode(),
                    semanticDigest,
                    confirm.getReportId(),
                    confirm.getConfirmerShipId()));
            return success("Ship " + confirm.getConfirmerShipId() + " had already confirmed report " + confirm.getReportId() + ".", record);
        }

        if (record.isTerminal()) {
            return failure("Report " + confirm.getReportId() + " is already in a terminal state.", record);
        }

        ConfirmationRecord confirmationRecord = new ConfirmationRecord(confirm, confirmPayloadDigest, referenceTimeMs);
        record.addConfirmation(confirmationRecord, referenceTimeMs);
        int requiredConfirmations = resolveRequiredConfirmations(confirm.getReportId());
        if (record.getValidConfirmationCount() >= requiredConfirmations) {
            record.setStatus(IncidentStatus.VERIFIED, referenceTimeMs);
        }

        processedRequests.put(confirm.getRequestId(), new IdempotencyRecord(
                confirm.getRequestId(),
                IncidentRequestType.CONFIRM.getCode(),
                semanticDigest,
                confirm.getReportId(),
                confirm.getConfirmerShipId()));

        LOGGER.info(String.format(
                "Committed CONFIRM for %s by %s, confirmations=%d/%d, confirmationRoot=%s, stateDigest=%s",
                confirm.getReportId(),
                confirm.getConfirmerShipId(),
                record.getValidConfirmationCount(),
                requiredConfirmations,
                HexUtils.toHex(record.getConfirmationRoot()),
                HexUtils.toHex(record.getStateDigest())));

        if (record.getStatus() == IncidentStatus.VERIFIED) {
            return success(
                    "Report " + confirm.getReportId() + " reached VERIFIED at "
                            + record.getValidConfirmationCount() + "/" + requiredConfirmations + ".",
                    record);
        }
        return success(
                "Confirmation from ship " + confirm.getConfirmerShipId() + " recorded for report "
                        + confirm.getReportId() + ". Confirmations: "
                        + record.getValidConfirmationCount() + "/" + requiredConfirmations,
                record);
    }

    private IncidentResponse handleGet(String reportId) {
        if (isBlank(reportId)) {
            return failure("reportId must not be blank.", null);
        }
        IncidentRecord record = reports.get(reportId);
        if (record == null) {
            return failure("Report " + reportId + " does not exist.", null);
        }
        return success("Found report " + reportId + ".", record);
    }

    private void expirePendingReports(long referenceTimeMs) {
        for (IncidentRecord record : reports.values()) {
            if (record.getStatus() == IncidentStatus.PENDING && record.getExpiresAtMs() < referenceTimeMs) {
                record.setStatus(IncidentStatus.EXPIRED, referenceTimeMs);
            }
        }
    }

    private int resolveRequiredConfirmations(String reportId) {
        Integer value = requiredConfirmationsByReportId.get(reportId);
        if (value == null || value.intValue() < 1) {
            throw new IllegalStateException("Missing requiredConfirmations for reportId " + reportId);
        }
        return value.intValue();
    }

    private byte[] computeLedgerHeadDigest() {
        CanonicalEncoder encoder = new CanonicalEncoder();
        encoder.writeByte(0x21);
        encoder.writeInt(reports.size());
        for (Map.Entry<String, IncidentRecord> entry : reports.entrySet()) {
            encoder.writeString(entry.getKey());
            encoder.writeByteArray(entry.getValue().getStateDigest());
            Integer required = requiredConfirmationsByReportId.get(entry.getKey());
            encoder.writeInt(required == null ? 0 : required.intValue());
        }
        return CryptoUtils.sha256(encoder.toByteArray());
    }

    private byte[] buildSubmitSemanticDigest(IncidentPayload payload, SubmitEnvelope submit, int requiredConfirmations) {
        CanonicalEncoder encoder = new CanonicalEncoder();
        payload.encode(encoder);
        encoder.writeByteArray(submit.toBeSignedBytes());
        encoder.writeInt(requiredConfirmations);
        return CryptoUtils.sha256(encoder.toByteArray());
    }

    private byte[] buildConfirmSemanticDigest(ConfirmEnvelope confirm) {
        return CryptoUtils.sha256(confirm.toBeSignedBytes());
    }

    private String validateEnvelopeClock(long issuedAtMs, long expiresAtMs, long referenceTimeMs) {
        if (expiresAtMs < issuedAtMs) {
            return "Envelope expiresAtMs must be greater than or equal to issuedAtMs.";
        }
        if (expiresAtMs < referenceTimeMs) {
            return "Envelope is already expired.";
        }
        if (issuedAtMs > referenceTimeMs + allowedClockSkewMs) {
            return "Envelope issuedAtMs is too far in the future.";
        }
        return null;
    }

    private IncidentResponse success(String message, IncidentRecord record) {
        return IncidentResponse.success(message, record, computeLedgerHeadDigest());
    }

    private IncidentResponse failure(String message, IncidentRecord record) {
        return IncidentResponse.failure(message, record, computeLedgerHeadDigest());
    }

    private static long resolveReferenceTime(MessageContext msgCtx) {
        return msgCtx == null ? System.currentTimeMillis() : msgCtx.getTimestamp();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
