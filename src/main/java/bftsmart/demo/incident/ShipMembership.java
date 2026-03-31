package bftsmart.demo.incident;

import java.util.TreeMap;

public final class ShipMembership {

    private final TreeMap<String, ShipIdentity> byShipId;
    private final TreeMap<String, ShipIdentity> byKeyId;

    private ShipMembership(TreeMap<String, ShipIdentity> byShipId, TreeMap<String, ShipIdentity> byKeyId) {
        this.byShipId = byShipId;
        this.byKeyId = byKeyId;
    }

    public static ShipMembership defaultMembership() {
        TreeMap<String, ShipIdentity> ships = new TreeMap<String, ShipIdentity>();
        TreeMap<String, ShipIdentity> keys = new TreeMap<String, ShipIdentity>();
        add(ships, keys, "ship-A");
        add(ships, keys, "ship-B");
        add(ships, keys, "ship-C");
        add(ships, keys, "ship-D");
        return new ShipMembership(ships, keys);
    }

    private static void add(TreeMap<String, ShipIdentity> ships, TreeMap<String, ShipIdentity> keys, String shipId) {
        ShipIdentity identity = new ShipIdentity(
                shipId,
                shipId + "-key-1",
                CryptoUtils.generateDeterministicEcKeyPair(shipId));
        ships.put(identity.getShipId(), identity);
        keys.put(identity.getKeyId(), identity);
    }

    public boolean isMember(String shipId) {
        return byShipId.containsKey(shipId);
    }

    public ShipIdentity getByShipId(String shipId) {
        return byShipId.get(shipId);
    }

    public ShipIdentity getByKeyId(String keyId) {
        return byKeyId.get(keyId);
    }

    public boolean shipOwnsKey(String shipId, String keyId) {
        ShipIdentity ship = byShipId.get(shipId);
        ShipIdentity keyOwner = byKeyId.get(keyId);
        return ship != null && ship == keyOwner;
    }
}
