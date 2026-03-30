package bftsmart.demo.incident;

import bftsmart.tom.ServiceProxy;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public final class IncidentClient {

    private IncidentClient() {
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length < 2) {
            printUsage();
            System.exit(-1);
        }

        int processId = Integer.parseInt(args[0]);
        String command = args[1].trim().toLowerCase();

        IncidentRequest request = buildRequest(command, args);

        try (ServiceProxy proxy = new ServiceProxy(processId)) {
            byte[] reply = proxy.invokeOrdered(IncidentMessageIO.toBytes(request));

            if (reply == null || reply.length == 0) {
                System.out.println("No reply received from the replicas.");
                return;
            }

            IncidentResponse response = IncidentMessageIO.fromBytes(reply, IncidentResponse.class);
            System.out.println(response.isSuccess() ? "[OK] " + response.getMessage() : "[ERROR] " + response.getMessage());
            if (response.getLedgerHeadHash() != null) {
                System.out.println("Ledger head: " + response.getLedgerHeadHash());
            }

            if (response.getRecord() != null) {
                printRecord(response.getRecord());
            }

            List<IncidentRecord> records = response.getRecords();
            if (records != null && !records.isEmpty()) {
                for (IncidentRecord record : records) {
                    System.out.println();
                    printRecord(record);
                }
            }
        }
    }

    private static IncidentRequest buildRequest(String command, String[] args) {
        IncidentRequest request = new IncidentRequest();

        if ("submit".equals(command)) {
            if (args.length < 9) {
                printUsage();
                System.exit(-1);
            }
            request.setType(IncidentRequestType.SUBMIT_REPORT);
            request.setReportId(args[2]);
            request.setShipId(args[3]);
            request.setLatitude(Double.parseDouble(args[4]));
            request.setLongitude(Double.parseDouble(args[5]));
            request.setConfirmationThreshold(Integer.parseInt(args[6]));
            request.setEvidenceHash(args[7]);
            request.setDescription(joinFrom(args, 8));
            return request;
        }

        if ("submit-pbft".equals(command) || "submitpbft".equals(command)) {
            if (args.length < 9) {
                printUsage();
                System.exit(-1);
            }
            request.setType(IncidentRequestType.SUBMIT_REPORT);
            request.setReportId(args[2]);
            request.setShipId(args[3]);
            request.setLatitude(Double.parseDouble(args[4]));
            request.setLongitude(Double.parseDouble(args[5]));
            request.setFaultTolerance(Integer.parseInt(args[6]));
            request.setEvidenceHash(args[7]);
            request.setDescription(joinFrom(args, 8));
            return request;
        }

        if ("confirm".equals(command)) {
            if (args.length < 4) {
                printUsage();
                System.exit(-1);
            }
            request.setType(IncidentRequestType.CONFIRM_REPORT);
            request.setReportId(args[2]);
            request.setShipId(args[3]);
            request.setEvidenceHash(args.length > 4 ? joinFrom(args, 4) : "");
            return request;
        }

        if ("get".equals(command)) {
            if (args.length < 3) {
                printUsage();
                System.exit(-1);
            }
            request.setType(IncidentRequestType.GET_REPORT);
            request.setReportId(args[2]);
            return request;
        }

        if ("list".equals(command)) {
            request.setType(IncidentRequestType.LIST_REPORTS);
            return request;
        }

        if ("head".equals(command)) {
            request.setType(IncidentRequestType.GET_LEDGER_HEAD);
            return request;
        }

        printUsage();
        System.exit(-1);
        return request;
    }

    private static void printRecord(IncidentRecord record) {
        System.out.println("Report ID: " + record.getReportId());
        System.out.println("Status: " + record.getStatus().name());
        System.out.println("Reporter: " + record.getReporterId());
        System.out.println("Location: " + IncidentRecord.formatCoordinate(record.getLatitude())
                + ", " + IncidentRecord.formatCoordinate(record.getLongitude()));
        if (record.getFaultTolerance() > 0) {
            System.out.println("PBFT parameters: n = " + record.getValidatorCount()
                    + ", f = " + record.getFaultTolerance()
                    + ", quorum = " + record.getConfirmationThreshold());
        }
        System.out.println("Confirmations: " + record.getConfirmationCount()
                + "/" + record.getConfirmationThreshold());
        System.out.println("Description: " + record.getDescription());
        System.out.println("Last mutation hash: " + record.getLastMutationHash());
        System.out.println("Witness evidence hashes:");
        for (Map.Entry<String, String> witness : record.getWitnessEvidenceHashes().entrySet()) {
            System.out.println("  " + witness.getKey() + " -> " + witness.getValue());
        }
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
        System.out.println("  IncidentClient <process id> submit <reportId> <shipId> <latitude> <longitude> <threshold> <evidenceHash> <description...>");
        System.out.println("  IncidentClient <process id> submit-pbft <reportId> <shipId> <latitude> <longitude> <f> <evidenceHash> <description...>");
        System.out.println("  IncidentClient <process id> confirm <reportId> <shipId> [<evidenceHash>]");
        System.out.println("  IncidentClient <process id> get <reportId>");
        System.out.println("  IncidentClient <process id> list");
        System.out.println("  IncidentClient <process id> head");
    }
}
