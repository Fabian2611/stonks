package io.fabianbuthere.stonks.api;

public enum BarrelMode {
    ONLY_DELIVERY("Delivery Only", "delivery"),
    ONLY_TRANSPORT_DROPOFF("Transport Dropoff", "transport_dropoff"),
    BOTH("Both Delivery & Dropoff", "both"),
    TRANSPORT_PICKUP("Transport Pickup", "transport_pickup");
    
    private final String displayName;
    private final String serializedName;
    
    BarrelMode(String displayName, String serializedName) {
        this.displayName = displayName;
        this.serializedName = serializedName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getSerializedName() {
        return serializedName;
    }
    
    public static BarrelMode fromSerializedName(String name) {
        for (BarrelMode mode : values()) {
            if (mode.serializedName.equals(name)) {
                return mode;
            }
        }
        return ONLY_DELIVERY; // default
    }
    
    public boolean allowsDelivery() {
        return this == ONLY_DELIVERY || this == BOTH;
    }
    
    public boolean allowsTransportDropoff() {
        return this == ONLY_TRANSPORT_DROPOFF || this == BOTH;
    }
    
    public boolean allowsTransportPickup() {
        return this == TRANSPORT_PICKUP;
    }
    
    public BarrelMode next() {
        BarrelMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
