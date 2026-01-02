package io.fabianbuthere.stonks.api.stock;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;

public class Company {
    private String name;
    private String symbol;
    private int shareCount;
    private double sharePercentage;
    private int[] profitsLast4Weeks;
    private int totalPropertySize;
    private Map<String, Double> bankAccounts;
    private String primaryBankAccount;
    private UUID ownerId;

    public Company(String name, String symbol, int shareCount, double sharePercentage, 
                   int[] profitsLast4Weeks, int totalPropertySize, UUID ownerId) {
        this.name = name;
        this.symbol = symbol;
        this.shareCount = shareCount;
        this.sharePercentage = sharePercentage;
        this.profitsLast4Weeks = profitsLast4Weeks;
        this.totalPropertySize = totalPropertySize;
        this.bankAccounts = new LinkedHashMap<>();
        this.ownerId = ownerId;
    }

    public Company(CompoundTag tag) {
        this.name = tag.getString("name");
        this.symbol = tag.getString("symbol");
        this.shareCount = tag.getInt("shareCount");
        this.sharePercentage = tag.getDouble("sharePercentage");
        this.soldShares = tag.getInt("soldShares");
        
        int[] profits = tag.getIntArray("profitsLast4Weeks");
        this.profitsLast4Weeks = profits.length == 4 ? profits : new int[]{0, 0, 0, 0};
        
        this.totalPropertySize = tag.getInt("totalPropertySize");
        this.primaryBankAccount = tag.getString("primaryBankAccount");
        
        if (tag.hasUUID("ownerId")) {
            this.ownerId = tag.getUUID("ownerId");
        }
        
        this.bankAccounts = new LinkedHashMap<>();
        ListTag accountsList = tag.getList("bankAccounts", Tag.TAG_COMPOUND);
        for (Tag accountTag : accountsList) {
            CompoundTag account = (CompoundTag) accountTag;
            bankAccounts.put(account.getString("name"), account.getDouble("balance"));
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("symbol", symbol);
        tag.putInt("shareCount", shareCount);
        tag.putDouble("sharePercentage", sharePercentage);
        tag.putInt("soldShares", soldShares);
        tag.putIntArray("profitsLast4Weeks", profitsLast4Weeks);
        tag.putInt("totalPropertySize", totalPropertySize);
        tag.putString("primaryBankAccount", primaryBankAccount);
        
        if (ownerId != null) {
            tag.putUUID("ownerId", ownerId);
        }
        
        ListTag accountsList = new ListTag();
        for (Map.Entry<String, Double> entry : bankAccounts.entrySet()) {
            CompoundTag account = new CompoundTag();
            account.putString("name", entry.getKey());
            account.putDouble("balance", entry.getValue());
            accountsList.add(account);
        }
        tag.put("bankAccounts", accountsList);
        
        return tag;
    }

    public double calculateValue(double worthPerSqm, double liquidCashMultiplier) {
        double propertyValue = totalPropertySize * worthPerSqm;
        double profitValue = Arrays.stream(profitsLast4Weeks).sum();
        double cashValue = bankAccounts.values().stream().mapToDouble(Double::doubleValue).sum() * liquidCashMultiplier;
        return propertyValue + profitValue + cashValue;
    }

    public void addProfit(double profit) {
        if (primaryBankAccount != null && bankAccounts.containsKey(primaryBankAccount)) {
            bankAccounts.put(primaryBankAccount, bankAccounts.get(primaryBankAccount) + profit);
        }
    }

    public void setBankAccountBalance(String accountName, double balance) {
        bankAccounts.put(accountName, balance);
    }

    public void addBankAccount(String accountName) {
        bankAccounts.put(accountName, 0.0);
        if (primaryBankAccount == null) {
            primaryBankAccount = accountName;
        }
    }

    public void removeBankAccount(String accountName) {
        bankAccounts.remove(accountName);
        if (accountName.equals(primaryBankAccount)) {
            primaryBankAccount = bankAccounts.isEmpty() ? null : bankAccounts.keySet().iterator().next();
        }
    }
    
    public void setBankAccountValue(String accountName, double value) {
        if (bankAccounts.containsKey(accountName)) {
            bankAccounts.put(accountName, value);
        }
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public int getShareCount() { return shareCount; }
    public void setShareCount(int shareCount) { this.shareCount = shareCount; }

    public double getSharePercentage() { return sharePercentage; }
    public void setSharePercentage(double sharePercentage) { this.sharePercentage = sharePercentage; }

    public int[] getProfitsLast4Weeks() { return profitsLast4Weeks; }
    public void setProfitsLast4Weeks(int[] profitsLast4Weeks) { this.profitsLast4Weeks = profitsLast4Weeks; }

    public int getTotalPropertySize() { return totalPropertySize; }
    public void setTotalPropertySize(int totalPropertySize) { this.totalPropertySize = totalPropertySize; }

    public Map<String, Double> getBankAccounts() { return bankAccounts; }

    public String getPrimaryBankAccount() { return primaryBankAccount; }
    public void setPrimaryBankAccount(String primaryBankAccount) { this.primaryBankAccount = primaryBankAccount; }

    public UUID getOwnerId() { return ownerId; }
    public void setOwnerId(UUID ownerId) { this.ownerId = ownerId; }

    public double getTotalCash() {
        return bankAccounts.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getPrimaryAccountBalance() {
        if (primaryBankAccount != null && bankAccounts.containsKey(primaryBankAccount)) {
            return bankAccounts.get(primaryBankAccount);
        }
        return 0.0;
    }
    
    private int soldShares = 0;
    
    public int calculateStockPrice() {
        double companyValue = calculateCompanyValue();
        if (shareCount <= 0) return 0;
        return (int) Math.floor((sharePercentage / 100.0) * companyValue / shareCount);
    }
    
    public double calculateCompanyValue() {
        io.fabianbuthere.stonks.config.StonksConfig config = io.fabianbuthere.stonks.config.StonksConfig.INSTANCE;
        double worthPerSqm = config.WORTH_PER_SQM.get();
        double liquidCashMultiplier = config.LIQUID_CASH_MULTIPLIER.get();
        
        double propertyValue = totalPropertySize * worthPerSqm;
        double profitsValue = 0;
        for (int profit : profitsLast4Weeks) {
            profitsValue += profit;
        }
        double cashValue = getTotalCash() * liquidCashMultiplier;
        
        return propertyValue + profitsValue + cashValue;
    }
    
    public int getAvailableShares() {
        return shareCount - soldShares;
    }
    
    public void sellShare() {
        if (soldShares < shareCount) {
            soldShares++;
        }
    }
    
    public void returnShare() {
        if (soldShares > 0) {
            soldShares--;
        }
    }
    
    public int getSoldShares() {
        return soldShares;
    }
    
    public void setSoldShares(int soldShares) {
        this.soldShares = Math.max(0, Math.min(soldShares, shareCount));
    }
}
