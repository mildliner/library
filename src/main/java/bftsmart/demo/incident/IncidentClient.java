package bftsmart.demo.incident;

import bftsmart.tom.ServiceProxy;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class IncidentClient {

    private IncidentClient() {
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(-1);
        }

        int processId = Integer.parseInt(args[0]);
        String command = args[1].trim().toLowerCase(Locale.ROOT);

        try (ServiceProxy proxy = new ServiceProxy(processId)) {
            IncidentRequest request = buildRequest(proxy, command, args);
            if (request == null) {
                return;
            }

            IncidentResponse response = invokeOrdered(proxy, request);
            if (response == null) {
                System.out.println("No reply received from the replicas.");
                return;
            }

            System.out.println(response.isSuccess() ? "[OK] " + response.getMessage() : "[ERROR] " + response.getMessage());
            if (response.getHeadDigest().length > 0) {
                System.out.println("Ledger head digest: " + HexUtils.toHex(response.getHeadDigest()));
            }

            if (response.getRecord() != null) {
                printRecord(response.getRecord());
            }

            if (!response.getRecords().isEmpty()) {
                for (IncidentRecord record : response.getRecords()) {
                    System.out.println();
                    printRecord(record);
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private static IncidentRequest buildRequest(ServiceProxy proxy, String command, String[] args) {
        if ("submit".equals(command)) {
            if (args.length < 9) {
                printUsage();
                System.exit(-1);
            }
            String reportId = args[2];
            String shipId = args[3];
            int latE7 = toScaledCoordinate(args[4]);
            int lonE7 = toScaledCoordinate(args[5]);
            int incidentTypeCode = Integer.parseInt(args[6]);
            int severityCode = Integer.parseInt(args[7]);
            int requiredConfirmations = Integer.parseInt(args[8]);
            String evidenceText = args.length > 9 ? joinFrom(args, 9) : "report-evidence";

            ShipIdentity identity = requireIdentity(shipId);
            long now = System.currentTimeMillis();
            byte[] reporterEvidenceRoot = buildReporterEvidenceRoot(shipId, latE7, lonE7, now, evidenceText);
            IncidentPayload payload = new IncidentPayload(
                    IncidentProtocol.SCHEMA_VERSION,
                    reportId,
                    shipId,
                    now,
                    now,
                    latE7,
                    lonE7,
                    incidentTypeCode,
                    severityCode,
                    reporterEvidenceRoot,
                    identity.getKeyId());
            SubmitEnvelope submit = SubmitEnvelope.sign(
                    payload,
                    UUID.randomUUID().toString(),
                    now,
                    now + IncidentProtocol.DEFAULT_TTL_MS,
                    identity);
            return IncidentRequest.submit(payload, submit, requiredConfirmations);
        }

        if ("confirm".equals(command)) {
            if (args.length < 5) {
                printUsage();
                System.exit(-1);
            }
            String reportId = args[2];
            String shipId = args[3];
            int decisionCode = Integer.parseInt(args[4]);
            String evidenceText = args.length > 5 ? joinFrom(args, 5) : "witness-evidence";

            ShipIdentity identity = requireIdentity(shipId);
            IncidentRecord existing = fetchReport(proxy, reportId);
            if (existing == null) {
                return null;
            }

            long now = System.currentTimeMillis();
            byte[] witnessEvidenceRoot = buildWitnessEvidenceRoot(shipId, decisionCode, now, evidenceText);
            ConfirmEnvelope confirm = ConfirmEnvelope.sign(
                    existing.getReportHash(),
                    reportId,
                    shipId,
                    witnessEvidenceRoot,
                    decisionCode,
                    UUID.randomUUID().toString(),
                    now,
                    now + IncidentProtocol.DEFAULT_TTL_MS,
                    identity);
            return IncidentRequest.confirm(confirm);
        }

        if ("get".equals(command)) {
            if (args.length < 3) {
                printUsage();
                System.exit(-1);
            }
            return IncidentRequest.get(args[2]);
        }

        if ("list".equals(command)) {
            return IncidentRequest.list();
        }

        if ("head".equals(command)) {
            return IncidentRequest.head();
        }

        printUsage();
        System.exit(-1);
        return null;
    }

    private static IncidentResponse invokeOrdered(ServiceProxy proxy, IncidentRequest request) {
        byte[] reply = proxy.invokeOrdered(request.toByteArray());
        if (reply == null || reply.length == 0) {
            return null;
        }
        return IncidentResponse.fromBytes(reply);
    }

    private static IncidentRecord fetchReport(ServiceProxy proxy, String reportId) {
        IncidentResponse response = invokeOrdered(proxy, IncidentRequest.get(reportId));
        if (response == null) {
            throw new IllegalStateException("No reply received while resolving report " + reportId + ".");
        }
        if (!response.isSuccess() || response.getRecord() == null) {
            throw new IllegalStateException("Cannot confirm missing report " + reportId + ": " + response.getMessage());
        }
        return response.getRecord();
    }

    private static ShipIdentity requireIdentity(String shipId) {
        ShipIdentity identity = ShipMembership.defaultMembership().getByShipId(shipId);
        if (identity == null) {
            throw new IllegalArgumentException("Unknown demo shipId: " + shipId);
        }
        return identity;
    }

    private static byte[] buildReporterEvidenceRoot(String shipId, int latE7, int lonE7, long eventTimeMs, String evidenceText) {
        EvidenceBundle bundle = new EvidenceBundle(Arrays.asList(
                new EvidenceLeaf(1, shipId + "-report-text", 1L, evidenceText.getBytes(StandardCharsets.UTF_8)),
                new EvidenceLeaf(2, shipId + "-report-location", 1L, encodeInts(latE7, lonE7)),
                new EvidenceLeaf(3, shipId + "-report-time", 1L, encodeLong(eventTimeMs))));
        return bundle.merkleRoot();
    }

    private static byte[] buildWitnessEvidenceRoot(String shipId, int decisionCode, long issuedAtMs, String evidenceText) {
        EvidenceBundle bundle = new EvidenceBundle(Arrays.asList(
                new EvidenceLeaf(1, shipId + "-confirm-text", 1L, evidenceText.getBytes(StandardCharsets.UTF_8)),
                new EvidenceLeaf(2, shipId + "-confirm-decision", 1L, encodeInt(decisionCode)),
                new EvidenceLeaf(3, shipId + "-confirm-time", 1L, encodeLong(issuedAtMs))));
        return bundle.merkleRoot();
    }

    private static byte[] encodeInt(int value) {
        CanonicalEncoder encoder = new CanonicalEncoder();
        encoder.writeInt(value);
        return encoder.toByteArray();
    }

    private static byte[] encodeInts(int first, int second) {
        CanonicalEncoder encoder = new CanonicalEncoder();
        encoder.writeInt(first);
        encoder.writeInt(second);
        return encoder.toByteArray();
    }

    private static byte[] encodeLong(long value) {
        CanonicalEncoder encoder = new CanonicalEncoder();
        encoder.writeLong(value);
        return encoder.toByteArray();
    }

    private static int toScaledCoordinate(String value) {
        return (int) Math.round(Double.parseDouble(value) * 10_000_000d);
    }

    private static void printRecord(IncidentRecord record) {
        IncidentPayload payload = record.getPayload();
        System.out.println("Report ID: " + record.getReportId());
        System.out.println("Status: " + record.getStatus().name());
        System.out.println("Reporter: " + record.getReporterShipId());
        System.out.println("Reporter key: " + payload.getReporterKeyId());
        System.out.println("Event time (ms): " + payload.getEventTimeMs());
        System.out.println("Reported at (ms): " + payload.getReportedAtMs());
        System.out.println("Location: " + formatCoordinate(payload.getLatE7()) + ", " + formatCoordinate(payload.getLonE7()));
        System.out.println("Incident type code: " + payload.getIncidentTypeCode());
        System.out.println("Severity code: " + payload.getSeverityCode());
        System.out.println("Valid confirmations: " + record.getValidConfirmationCount());
        System.out.println("Report hash: " + HexUtils.toHex(record.getReportHash()));
        System.out.println("Reporter evidence root: " + HexUtils.toHex(payload.getReporterEvidenceRoot()));
        System.out.println("Confirmation root: " + HexUtils.toHex(record.getConfirmationRoot()));
        System.out.println("State digest: " + HexUtils.toHex(record.getStateDigest()));
        System.out.println("Submit request ID: " + record.getSubmit().getRequestId());
        System.out.println("Created at (ms): " + record.getCreatedAtMs());
        System.out.println("Updated at (ms): " + record.getUpdatedAtMs());
        System.out.println("Confirmations by ship:");
        for (Map.Entry<String, ConfirmationRecord> entry : record.getConfirmationsByShipId().entrySet()) {
            ConfirmationRecord confirmation = entry.getValue();
            System.out.println("  " + entry.getKey()
                    + " decision=" + confirmation.getDecisionCode()
                    + " witnessRoot=" + HexUtils.toHex(confirmation.getWitnessEvidenceRoot())
                    + " payloadDigest=" + HexUtils.toHex(confirmation.getConfirmPayloadDigest()));
        }
    }

    private static String formatCoordinate(int coordinateE7) {
        return String.format(Locale.ROOT, "%.7f", coordinateE7 / 10_000_000d);
    }

    private static String joinFrom(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  IncidentClient <process id> submit <reportId> <shipId> <latitude> <longitude> <incidentTypeCode> <severityCode> <requiredConfirmations> [<evidenceText...>]");
        System.out.println("  IncidentClient <process id> confirm <reportId> <shipId> <decisionCode> [<evidenceText...>]");
        System.out.println("  IncidentClient <process id> get <reportId>");
        System.out.println("  IncidentClient <process id> list");
        System.out.println("  IncidentClient <process id> head");
    }
}
