package bftsmart.demo.incident;

public final class PbftMath {

    private PbftMath() {
    }

    public static int validatorCountForFaultTolerance(int faultyNodes) {
        if (faultyNodes < 1) {
            throw new IllegalArgumentException("faultTolerance must be at least 1.");
        }
        return (3 * faultyNodes) + 1;
    }

    public static int quorumForFaultTolerance(int faultyNodes) {
        if (faultyNodes < 1) {
            throw new IllegalArgumentException("faultTolerance must be at least 1.");
        }
        return (2 * faultyNodes) + 1;
    }
}
