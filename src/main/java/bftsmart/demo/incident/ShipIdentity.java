package bftsmart.demo.incident;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public final class ShipIdentity {

    private final String shipId;
    private final String keyId;
    private final KeyPair keyPair;

    public ShipIdentity(String shipId, String keyId, KeyPair keyPair) {
        this.shipId = shipId;
        this.keyId = keyId;
        this.keyPair = keyPair;
    }

    public String getShipId() {
        return shipId;
    }

    public String getKeyId() {
        return keyId;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }
}
