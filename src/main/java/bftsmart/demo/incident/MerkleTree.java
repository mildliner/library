package bftsmart.demo.incident;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class MerkleTree {

    private static final Comparator<ConfirmationRecord> CONFIRMATION_ORDER = new Comparator<ConfirmationRecord>() {
        @Override
        public int compare(ConfirmationRecord left, ConfirmationRecord right) {
            return left.getConfirmerShipId().compareTo(right.getConfirmerShipId());
        }
    };

    private MerkleTree() {
    }

    public static byte[] computeEvidenceRoot(List<EvidenceLeaf> leaves) {
        if (leaves == null || leaves.isEmpty()) {
            throw new IllegalArgumentException("Evidence bundle must not be empty");
        }
        ArrayList<byte[]> digests = new ArrayList<byte[]>(leaves.size());
        for (EvidenceLeaf leaf : leaves) {
            digests.add(hashLeaf(leaf.canonicalLeafBytes()));
        }
        return buildRoot(digests);
    }

    public static byte[] computeConfirmationRoot(Collection<ConfirmationRecord> confirmations) {
        if (confirmations == null || confirmations.isEmpty()) {
            return CryptoUtils.zeroDigest();
        }
        ArrayList<ConfirmationRecord> sorted = new ArrayList<ConfirmationRecord>(confirmations);
        Collections.sort(sorted, CONFIRMATION_ORDER);
        ArrayList<byte[]> digests = new ArrayList<byte[]>(sorted.size());
        for (ConfirmationRecord confirmation : sorted) {
            digests.add(hashLeaf(confirmation.toMerkleLeafBytes()));
        }
        return buildRoot(digests);
    }

    private static byte[] buildRoot(List<byte[]> digests) {
        ArrayList<byte[]> layer = new ArrayList<byte[]>(digests);
        while (layer.size() > 1) {
            if ((layer.size() & 1) == 1) {
                layer.add(layer.get(layer.size() - 1));
            }
            ArrayList<byte[]> next = new ArrayList<byte[]>(layer.size() / 2);
            for (int i = 0; i < layer.size(); i += 2) {
                next.add(hashParent(layer.get(i), layer.get(i + 1)));
            }
            layer = next;
        }
        return layer.get(0);
    }

    private static byte[] hashLeaf(byte[] canonicalLeafBytes) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(0x00);
        output.write(canonicalLeafBytes, 0, canonicalLeafBytes.length);
        return CryptoUtils.sha256(output.toByteArray());
    }

    private static byte[] hashParent(byte[] left, byte[] right) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(0x01);
        output.write(left, 0, left.length);
        output.write(right, 0, right.length);
        return CryptoUtils.sha256(output.toByteArray());
    }
}
