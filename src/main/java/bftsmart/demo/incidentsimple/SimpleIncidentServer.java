package bftsmart.demo.incidentsimple;

import bftsmart.tom.MessageContext;
import bftsmart.tom.ServiceReplica;
import bftsmart.tom.server.defaultservices.DefaultSingleRecoverable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SimpleIncidentServer extends DefaultSingleRecoverable {

    private static final Logger LOGGER = Logger.getLogger(SimpleIncidentServer.class.getName());

    private TreeMap<String, SimpleIncidentRecord> incidents = new TreeMap<String, SimpleIncidentRecord>();
    private TreeMap<String, Integer> thresholdsByIncidentId = new TreeMap<String, Integer>();

    public SimpleIncidentServer(int processId) {
        new ServiceReplica(processId, this, this);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Use: java SimpleIncidentServer <processId>");
            System.exit(-1);
        }
        new SimpleIncidentServer(Integer.parseInt(args[0]));
    }

    @Override
    public byte[] appExecuteOrdered(byte[] command, MessageContext msgCtx) {
        try {
            SimpleIncidentRequest request = SimpleIncidentMessageIO.fromBytes(command, SimpleIncidentRequest.class);
            logPbftFlow("request received", request, msgCtx);
            logPbftFlow("pre-prepare", request, msgCtx);
            logPbftFlow("prepare", request, msgCtx);
            logPbftFlow("commit", request, msgCtx);
            SimpleIncidentResponse response = executeOrdered(request);
            logPbftFlow("execute", request, msgCtx);
            logPbftFlow("reply", request, msgCtx);
            return SimpleIncidentMessageIO.toBytes(response);
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to process ordered request", e);
            try {
                return SimpleIncidentMessageIO.toBytes(SimpleIncidentResponse.failure("Failed to process ordered request: " + e.getMessage()));
            } catch (IOException ioException) {
                return new byte[0];
            }
        }
    }

    @Override
    public byte[] appExecuteUnordered(byte[] command, MessageContext msgCtx) {
        return appExecuteOrdered(command, msgCtx);
    }

    @Override
    public void installSnapshot(byte[] state) {
        try {
            ObjectInputStream objectIn = new ObjectInputStream(new ByteArrayInputStream(state));
            incidents = castTreeMap(objectIn.readObject());
            thresholdsByIncidentId = castThresholdMap(objectIn.readObject());
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to install simple incident snapshot", e);
        }
    }

    @Override
    public byte[] getSnapshot() {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
            objectOut.writeObject(incidents);
            objectOut.writeObject(thresholdsByIncidentId);
            objectOut.flush();
            return byteOut.toByteArray();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to take simple incident snapshot", e);
            return new byte[0];
        }
    }

    private SimpleIncidentResponse executeOrdered(SimpleIncidentRequest request) {
        switch (request.getType()) {
            case SUBMIT_INCIDENT:
                return handleSubmit(request);
            case CONFIRM_INCIDENT:
                return handleConfirm(request);
            case GET_INCIDENT:
                return handleGet(request);
            case LIST_INCIDENTS:
                return SimpleIncidentResponse.success(
                        "Listed " + incidents.size() + " incident(s).",
                        new ArrayList<SimpleIncidentRecord>(incidents.values()));
            default:
                return SimpleIncidentResponse.failure("Unsupported request type: " + request.getType());
        }
    }

    private SimpleIncidentResponse handleSubmit(SimpleIncidentRequest request) {
        String incidentId = normalize(request.getIncidentId());
        String reporterShipId = normalize(request.getShipId());
        String description = normalizeDescription(request.getDescription());
        int threshold = request.getConfirmationThreshold();

        if (incidentId == null) {
            return SimpleIncidentResponse.failure("incidentId must not be blank.");
        }
        if (reporterShipId == null) {
            return SimpleIncidentResponse.failure("reporterShipId must not be blank.");
        }
        if (threshold < 1) {
            return SimpleIncidentResponse.failure("confirmationThreshold must be at least 1.");
        }
        if (incidents.containsKey(incidentId)) {
            return SimpleIncidentResponse.failure("Incident " + incidentId + " already exists.");
        }

        SimpleIncidentRecord record = new SimpleIncidentRecord(
                incidentId,
                reporterShipId,
                description,
                request.getCreatedAt());
        incidents.put(incidentId, record);
        thresholdsByIncidentId.put(incidentId, Integer.valueOf(threshold));
        return SimpleIncidentResponse.success(
                "Incident " + incidentId + " submitted. confirmations=0/" + threshold,
                record);
    }

    private SimpleIncidentResponse handleConfirm(SimpleIncidentRequest request) {
        String incidentId = normalize(request.getIncidentId());
        String confirmerShipId = normalize(request.getShipId());

        if (incidentId == null) {
            return SimpleIncidentResponse.failure("incidentId must not be blank.");
        }
        if (confirmerShipId == null) {
            return SimpleIncidentResponse.failure("confirmerShipId must not be blank.");
        }

        SimpleIncidentRecord record = incidents.get(incidentId);
        if (record == null) {
            return SimpleIncidentResponse.failure("Incident " + incidentId + " does not exist.");
        }
        if (record.getReporterShipId().equals(confirmerShipId)) {
            return SimpleIncidentResponse.failure("Reporter cannot confirm its own incident in the simple demo.");
        }

        int threshold = thresholdsByIncidentId.get(incidentId).intValue();
        if (!record.addConfirmer(confirmerShipId, threshold)) {
            return SimpleIncidentResponse.success(
                    "Ship " + confirmerShipId + " had already confirmed incident " + incidentId + ".",
                    record);
        }

        if (record.getStatus() == SimpleIncidentStatus.VERIFIED) {
            return SimpleIncidentResponse.success(
                    "Incident " + incidentId + " reached VERIFIED at " + record.getConfirmationCount() + "/" + threshold + ".",
                    record);
        }
        return SimpleIncidentResponse.success(
                "Confirmation from ship " + confirmerShipId + " recorded for incident " + incidentId
                        + ". confirmations=" + record.getConfirmationCount() + "/" + threshold,
                record);
    }

    private SimpleIncidentResponse handleGet(SimpleIncidentRequest request) {
        String incidentId = normalize(request.getIncidentId());
        if (incidentId == null) {
            return SimpleIncidentResponse.failure("incidentId must not be blank.");
        }
        SimpleIncidentRecord record = incidents.get(incidentId);
        if (record == null) {
            return SimpleIncidentResponse.failure("Incident " + incidentId + " does not exist.");
        }
        return SimpleIncidentResponse.success("Found incident " + incidentId + ".", record);
    }

    private void logPbftFlow(String stage, SimpleIncidentRequest request, MessageContext msgCtx) {
        String consensusId = msgCtx == null ? "n/a" : Integer.toString(msgCtx.getConsensusId());
        String sender = msgCtx == null ? "n/a" : Integer.toString(msgCtx.getSender());
        LOGGER.info(String.format(
                "[PBFT FLOW] stage=%s consensusId=%s sender=%s operation=%s incidentId=%s",
                stage,
                consensusId,
                sender,
                request.getType().name(),
                request.getIncidentId()));
    }

    @SuppressWarnings("unchecked")
    private static TreeMap<String, SimpleIncidentRecord> castTreeMap(Object value) {
        return (TreeMap<String, SimpleIncidentRecord>) value;
    }

    @SuppressWarnings("unchecked")
    private static TreeMap<String, Integer> castThresholdMap(Object value) {
        return (TreeMap<String, Integer>) value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeDescription(String value) {
        return value == null ? "" : value.trim();
    }
}
