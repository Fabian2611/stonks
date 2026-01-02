package io.fabianbuthere.stonks.api.stock;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

public class Stock {
    private final UUID stockId;
    private final String companySymbol;
    private final long purchaseTime;
    private final int purchasePrice;
    private final UUID ownerUUID;

    public Stock(UUID stockId, String companySymbol, long purchaseTime, int purchasePrice, UUID ownerUUID) {
        this.stockId = stockId;
        this.companySymbol = companySymbol;
        this.purchaseTime = purchaseTime;
        this.purchasePrice = purchasePrice;
        this.ownerUUID = ownerUUID;
    }

    public UUID getStockId() {
        return stockId;
    }

    public String getCompanySymbol() {
        return companySymbol;
    }

    public long getPurchaseTime() {
        return purchaseTime;
    }

    public int getPurchasePrice() {
        return purchasePrice;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public boolean isFrozen(long currentTime) {
        long fiveDaysInMillis = 5L * 24L * 60L * 60L * 1000L;
        return (currentTime - purchaseTime) < fiveDaysInMillis;
    }

    /**
     * Calculates the depreciation multiplier based on age.
     * Every 7 days, the stock value decreases by 2%.
     * Formula: 0.98^(weeks_passed)
     */
    public double getDepreciationMultiplier(long currentTime) {
        long ageInMillis = currentTime - purchaseTime;
        double weeksPassed = ageInMillis / (7.0 * 24.0 * 60.0 * 60.0 * 1000.0);
        return Math.pow(0.98, weeksPassed);
    }

    /**
     * Calculates the current value of this stock including depreciation
     */
    public int getCurrentValue(int currentCompanyStockPrice, long currentTime) {
        double multiplier = getDepreciationMultiplier(currentTime);
        return (int) Math.floor(currentCompanyStockPrice * multiplier);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("StockId", stockId);
        tag.putString("CompanySymbol", companySymbol);
        tag.putLong("PurchaseTime", purchaseTime);
        tag.putInt("PurchasePrice", purchasePrice);
        tag.putUUID("OwnerUUID", ownerUUID);
        return tag;
    }

    public static Stock fromNBT(CompoundTag tag) {
        return new Stock(
            tag.getUUID("StockId"),
            tag.getString("CompanySymbol"),
            tag.getLong("PurchaseTime"),
            tag.getInt("PurchasePrice"),
            tag.getUUID("OwnerUUID")
        );
    }
}
