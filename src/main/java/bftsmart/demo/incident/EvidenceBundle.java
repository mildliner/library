package bftsmart.demo.incident;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class EvidenceBundle {

    private static final Comparator<EvidenceLeaf> SORT_ORDER = new Comparator<EvidenceLeaf>() {
        @Override
        public int compare(EvidenceLeaf left, EvidenceLeaf right) {
            int compareSource = Integer.compare(left.getSourceTypeCode(), right.getSourceTypeCode());
            if (compareSource != 0) {
                return compareSource;
            }
            int compareSensor = left.getSensorId().compareTo(right.getSensorId());
            if (compareSensor != 0) {
                return compareSensor;
            }
            return Long.compare(left.getSequenceNo(), right.getSequenceNo());
        }
    };

    private final ArrayList<EvidenceLeaf> leaves;

    public EvidenceBundle(List<EvidenceLeaf> leaves) {
        if (leaves == null || leaves.isEmpty()) {
            throw new IllegalArgumentException("Evidence bundle must not be empty");
        }
        this.leaves = new ArrayList<EvidenceLeaf>(leaves);
        Collections.sort(this.leaves, SORT_ORDER);
    }

    public List<EvidenceLeaf> getLeaves() {
        return new ArrayList<EvidenceLeaf>(leaves);
    }

    public byte[] merkleRoot() {
        return MerkleTree.computeEvidenceRoot(leaves);
    }
}
