package bftsmart.demo.incidentsimple;

import bftsmart.tom.ServiceProxy;

import java.io.IOException;
import java.util.List;

public final class SimpleIncidentClient {

    private SimpleIncidentClient() {
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        if (args.length < 2) {
            printUsage();
            System.exit(-1);
        }

        int processId = Integer.parseInt(args[0]);
        String command = args[1].trim().toLowerCase();
        SimpleIncidentRequest request = buildRequest(command, args);

        try (ServiceProxy proxy = new ServiceProxy(processId)) {
            byte[] reply = proxy.invokeOrdered(SimpleIncidentMessageIO.toBytes(request));
            if (reply == null || reply.length == 0) {
                System.out.println("No reply received from the replicas.");
                return;
            }

            SimpleIncidentResponse response = SimpleIncidentMessageIO.fromBytes(reply, SimpleIncidentResponse.class);
            System.out.println(response.isSuccess() ? "[OK] " + response.getMessage() : "[ERROR] " + response.getMessage());
            if (response.getRecord() != null) {
                printRecord(response.getRecord());
            }

            List<SimpleIncidentRecord> records = response.getRecords();
            if (!records.isEmpty()) {
                for (SimpleIncidentRecord record : records) {
                    System.out.println();
                    printRecord(record);
                }
            }
        }
    }

    private static SimpleIncidentRequest buildRequest(String command, String[] args) {
        if ("submit".equals(command) || "submit-incident".equals(command)) {
            if (args.length < 6) {
                printUsage();
                System.exit(-1);
            }
            return SimpleIncidentRequest.submit(args[2], args[3], joinFrom(args, 5), Integer.parseInt(args[4]));
        }

        if ("confirm".equals(command) || "confirm-incident".equals(command)) {
            if (args.length < 4) {
                printUsage();
                System.exit(-1);
            }
            return SimpleIncidentRequest.confirm(args[2], args[3]);
        }

        if ("get".equals(command) || "get-incident".equals(command)) {
            if (args.length < 3) {
                printUsage();
                System.exit(-1);
            }
            return SimpleIncidentRequest.get(args[2]);
        }

        if ("list".equals(command) || "list-incidents".equals(command)) {
            return SimpleIncidentRequest.list();
        }

        printUsage();
        System.exit(-1);
        return null;
    }

    private static void printRecord(SimpleIncidentRecord record) {
        System.out.println("Incident ID: " + record.getIncidentId());
        System.out.println("Reporter: " + record.getReporterShipId());
        System.out.println("Description: " + record.getDescription());
        System.out.println("Created at: " + record.getCreatedAt());
        System.out.println("Status: " + record.getStatus().name());
        System.out.println("Confirm ships: " + record.getConfirmShips());
        System.out.println("Confirmation count: " + record.getConfirmationCount());
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
        System.out.println("  SimpleIncidentClient <process id> submit-incident <incidentId> <reporterShipId> <confirmationThreshold> <description...>");
        System.out.println("  SimpleIncidentClient <process id> confirm-incident <incidentId> <confirmerShipId>");
        System.out.println("  SimpleIncidentClient <process id> get-incident <incidentId>");
        System.out.println("  SimpleIncidentClient <process id> list-incidents");
    }
}
