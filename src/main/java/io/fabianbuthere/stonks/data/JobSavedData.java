package io.fabianbuthere.stonks.data;

import io.fabianbuthere.stonks.api.DeliveryJob;
import io.fabianbuthere.stonks.api.JobData;
import io.fabianbuthere.stonks.api.TransportJob;
import io.fabianbuthere.stonks.api.stock.Company;
import io.fabianbuthere.stonks.api.stock.Stock;
import io.fabianbuthere.stonks.config.StonksConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.*;

public class JobSavedData extends SavedData {
    private static final String DATA_NAME = "stonks_jobs";
    
    public JobData jobData;
    public List<Company> companies;
    public Map<UUID, Stock> activeStocks;
    public BlockPos stockSignMatrixPos;
    public int stockSignMatrixWidth;
    public int stockSignMatrixHeight;
    public long lastDepreciationTime;

    public JobSavedData() {
        this.jobData = new JobData();
        this.companies = new ArrayList<>();
        this.activeStocks = new HashMap<>();
        this.stockSignMatrixPos = null;
        this.stockSignMatrixWidth = 0;
        this.stockSignMatrixHeight = 0;
        this.lastDepreciationTime = System.currentTimeMillis();
    }

    public JobSavedData(CompoundTag tag) {
        load(tag);
    }

    public void load(CompoundTag compoundTag) {
        jobData = new JobData();
        jobData.deserialize(compoundTag);
        
        companies = new ArrayList<>();
        ListTag companiesList = compoundTag.getList("companies", Tag.TAG_COMPOUND);
        for (Tag companyTag : companiesList) {
            companies.add(new Company((CompoundTag) companyTag));
        }
        
        activeStocks = new HashMap<>();
        ListTag stocksList = compoundTag.getList("activeStocks", Tag.TAG_COMPOUND);
        for (Tag stockTag : stocksList) {
            Stock stock = Stock.fromNBT((CompoundTag) stockTag);
            activeStocks.put(stock.getStockId(), stock);
        }
        
        if (compoundTag.contains("stockSignMatrixPos")) {
            stockSignMatrixPos = BlockPos.of(compoundTag.getLong("stockSignMatrixPos"));
            stockSignMatrixWidth = compoundTag.getInt("stockSignMatrixWidth");
            stockSignMatrixHeight = compoundTag.getInt("stockSignMatrixHeight");
        }
        
        lastDepreciationTime = compoundTag.getLong("lastDepreciationTime");
        if (lastDepreciationTime == 0) {
            lastDepreciationTime = System.currentTimeMillis();
        }
    }

    @Override
    public @Nonnull CompoundTag save(@Nonnull CompoundTag compoundTag) {
        jobData.serialize(compoundTag);
        
        ListTag companiesList = new ListTag();
        for (Company company : companies) {
            companiesList.add(company.save());
        }
        compoundTag.put("companies", companiesList);
        
        ListTag stocksList = new ListTag();
        for (Stock stock : activeStocks.values()) {
            stocksList.add(stock.toNBT());
        }
        compoundTag.put("activeStocks", stocksList);
        
        if (stockSignMatrixPos != null) {
            compoundTag.putLong("stockSignMatrixPos", stockSignMatrixPos.asLong());
            compoundTag.putInt("stockSignMatrixWidth", stockSignMatrixWidth);
            compoundTag.putInt("stockSignMatrixHeight", stockSignMatrixHeight);
        }
        
        compoundTag.putLong("lastDepreciationTime", lastDepreciationTime);
        
        return compoundTag;
    }

    public static JobSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                JobSavedData::new,
                JobSavedData::new,
                "stonks_jobs_data"
        );
    }

    public JobData getJobData() {
        return jobData;
    }

    public void addNewDeliveryJob(DeliveryJob job) {
        this.jobData.activeJobs.add(job);
        this.setDirty();
    }

    public void addNewTransportJob(TransportJob job) {
        this.jobData.activeTransports.add(job);
        this.setDirty();
    }

    public void deleteAllJobs() {
        this.jobData.activeJobs.clear();
        this.jobData.activeTransports.clear();
        this.jobData.knownJobs.clear();
        this.jobData.transportKnownJobs.clear();
        this.jobData.transportPickedUp.clear();
        this.jobData.deliveryConfirmed.clear();
        this.setDirty();
    }

    public boolean deleteDeliveryJob(int index) {
        if (index < 0 || index >= this.jobData.activeJobs.size()) return false;
        this.jobData.activeJobs.remove(index);
        java.util.Map<Integer, java.util.UUID> newMap = new java.util.HashMap<>();
        for (var e : this.jobData.knownJobs.entrySet()) {
            int k = e.getKey();
            java.util.UUID v = e.getValue();
            if (k == index) continue;
            newMap.put(k > index ? k - 1 : k, v);
        }
        this.jobData.knownJobs = newMap;
        
        // Clean up deliveryConfirmed set
        java.util.Set<Integer> newConfirmed = new java.util.HashSet<>();
        for (Integer idx : this.jobData.deliveryConfirmed) {
            if (idx == index) continue;
            newConfirmed.add(idx > index ? idx - 1 : idx);
        }
        this.jobData.deliveryConfirmed = newConfirmed;
        
        this.setDirty();
        return true;
    }

    public boolean deleteTransportJob(int index) {
        if (index < 0 || index >= this.jobData.activeTransports.size()) return false;
        this.jobData.activeTransports.remove(index);
        java.util.Map<Integer, java.util.UUID> newMap = new java.util.HashMap<>();
        for (var e : this.jobData.transportKnownJobs.entrySet()) {
            int k = e.getKey();
            java.util.UUID v = e.getValue();
            if (k == index) continue;
            newMap.put(k > index ? k - 1 : k, v);
        }
        this.jobData.transportKnownJobs = newMap;
        
        // Clean up transportPickedUp set
        java.util.Set<Integer> newPickedUp = new java.util.HashSet<>();
        for (Integer idx : this.jobData.transportPickedUp) {
            if (idx == index) continue;
            newPickedUp.add(idx > index ? idx - 1 : idx);
        }
        this.jobData.transportPickedUp = newPickedUp;
        
        this.setDirty();
        return true;
    }

    public boolean acceptJobForPlayer(ServerPlayer player, int jobId) {
        if (jobId >= this.jobData.activeJobs.size()) {
            return false;
        }

        if (jobData.knownJobs.containsKey(jobId)) return false; // already accepted

        java.util.UUID playerId = player.getUUID();
        long perCategoryLimit = StonksConfig.MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_DELIVERING.get();
        long totalLimit = StonksConfig.MAX_ACCEPTED_TOTAL_PER_PLAYER.get();

        long acceptedDelivery = jobData.knownJobs.values().stream().filter(u -> u.equals(playerId)).count();
        long acceptedTransport = jobData.transportKnownJobs.values().stream().filter(u -> u.equals(playerId)).count();
        long totalAccepted = acceptedDelivery + acceptedTransport;

        if (acceptedDelivery >= perCategoryLimit) return false;
        if (totalAccepted >= totalLimit) return false;

        jobData.knownJobs.put(jobId, playerId);
        this.setDirty();
        return true;
    }

    public boolean acceptTransportForPlayer(ServerPlayer player, int jobId) {
        if (jobId >= this.jobData.activeTransports.size()) {
            return false;
        }

        if (jobData.transportKnownJobs.containsKey(jobId)) return false; // already accepted

        java.util.UUID playerId = player.getUUID();
        long perCategoryLimit = StonksConfig.MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_TRANSPORT.get();
        long totalLimit = StonksConfig.MAX_ACCEPTED_TOTAL_PER_PLAYER.get();

        long acceptedDelivery = jobData.knownJobs.values().stream().filter(u -> u.equals(playerId)).count();
        long acceptedTransport = jobData.transportKnownJobs.values().stream().filter(u -> u.equals(playerId)).count();
        long totalAccepted = acceptedDelivery + acceptedTransport;

        if (acceptedTransport >= perCategoryLimit) return false;
        if (totalAccepted >= totalLimit) return false;

        jobData.transportKnownJobs.put(jobId, playerId);
        jobData.transportAcceptedTime.put(jobId, System.currentTimeMillis()); // Track acceptance time
        this.setDirty();
        return true;
    }

    public boolean cancelDeliveryForPlayer(ServerPlayer player, int jobId) {
        if (jobId >= this.jobData.activeJobs.size()) return false;
        java.util.UUID pid = player.getUUID();
        if (!jobData.knownJobs.containsKey(jobId)) return false;
        if (!jobData.knownJobs.get(jobId).equals(pid)) return false;
        jobData.knownJobs.remove(jobId);
        this.setDirty();
        return true;
    }

    public boolean cancelTransportForPlayer(ServerPlayer player, int jobId) {
        if (jobId >= this.jobData.activeTransports.size()) return false;
        java.util.UUID pid = player.getUUID();
        if (!jobData.transportKnownJobs.containsKey(jobId)) return false;
        if (!jobData.transportKnownJobs.get(jobId).equals(pid)) return false;
        jobData.transportKnownJobs.remove(jobId);
        this.setDirty();
        return true;
    }

    public void serverTick(ServerLevel level) {
        long now = System.currentTimeMillis();
        int interval = StonksConfig.JOB_GENERATION_INTERVAL_SECONDS.get();
        boolean intervalPassed = now - jobData.lastGenerationMillis >= ((long) interval) * 1000L;
        
        // Check for company value depreciation every 7 days
        long depreciationInterval = 7L * 24L * 60L * 60L * 1000L; // 7 days in milliseconds
        if (now - lastDepreciationTime >= depreciationInterval) {
            applyCompanyDepreciation();
            lastDepreciationTime = now;
            this.setDirty();
        }
        
        int expirationMinutes = StonksConfig.JOB_EXPIRATION_MINUTES.get();

        java.util.Random rnd = new java.util.Random();

        int maxDelivering = StonksConfig.MAX_JOBS_PER_CATEGORY_DELIVERING.get();
        int maxTransport = StonksConfig.MAX_JOBS_PER_CATEGORY_TRANSPORT.get();
        int minDelivering = StonksConfig.MIN_JOBS_PER_CATEGORY_DELIVERING.get();
        int minTransport = StonksConfig.MIN_JOBS_PER_CATEGORY_TRANSPORT.get();
        
        // Check for expired jobs and remove them
        boolean anyExpired = false;
        
        // Remove expired delivery jobs that aren't taken
        java.util.Iterator<DeliveryJob> deliveryIter = jobData.activeJobs.iterator();
        int deliveryIndex = 0;
        while (deliveryIter.hasNext()) {
            DeliveryJob job = deliveryIter.next();
            boolean isTaken = jobData.knownJobs.containsKey(deliveryIndex);
            
            if (!isTaken && job.isExpired(now, expirationMinutes)) {
                deliveryIter.remove();
                anyExpired = true;
                
                // Clean up indices in knownJobs map
                java.util.Map<Integer, java.util.UUID> newMap = new java.util.HashMap<>();
                for (var e : jobData.knownJobs.entrySet()) {
                    int idx = e.getKey();
                    if (idx > deliveryIndex) {
                        newMap.put(idx - 1, e.getValue());
                    } else if (idx < deliveryIndex) {
                        newMap.put(idx, e.getValue());
                    }
                }
                jobData.knownJobs = newMap;
            } else {
                deliveryIndex++;
            }
        }
        
        // Remove expired transport jobs that aren't taken
        java.util.Iterator<io.fabianbuthere.stonks.api.TransportJob> transportIter = jobData.activeTransports.iterator();
        int transportIndex = 0;
        while (transportIter.hasNext()) {
            io.fabianbuthere.stonks.api.TransportJob job = transportIter.next();
            boolean isTaken = jobData.transportKnownJobs.containsKey(transportIndex);
            
            if (!isTaken && job.isExpired(now, expirationMinutes)) {
                transportIter.remove();
                anyExpired = true;
                
                // Clean up indices in transportKnownJobs map
                java.util.Map<Integer, java.util.UUID> newMap = new java.util.HashMap<>();
                for (var e : jobData.transportKnownJobs.entrySet()) {
                    int idx = e.getKey();
                    if (idx > transportIndex) {
                        newMap.put(idx - 1, e.getValue());
                    } else if (idx < transportIndex) {
                        newMap.put(idx, e.getValue());
                    }
                }
                jobData.transportKnownJobs = newMap;
            } else {
                transportIndex++;
            }
        }
        
        if (anyExpired) {
            this.setDirty();
        }

        // Try to generate a delivery job (immediately if below minimum, or on interval)
        if (jobData.activeJobs.size() < maxDelivering) {
            boolean shouldGenerate = (jobData.activeJobs.size() < minDelivering) || intervalPassed;
            if (shouldGenerate && !jobData.deliveryLocations.isEmpty()) {
                try {
                    DeliveryJob d = io.fabianbuthere.stonks.api.util.JobPool.getInstance().getRandomDeliveryJob();
                    int locIndex = rnd.nextInt(jobData.deliveryLocations.size());
                    DeliveryJob job = new DeliveryJob(d.parts(), false, d.payment(), locIndex, System.currentTimeMillis());
                    jobData.activeJobs.add(job);
                    this.setDirty();
                } catch (IllegalStateException e) {
                    // No jobs configured in JobPool, skip generation silently
                }
            }
        }
        
        // Try to generate a transport job (immediately if below minimum, or on interval)
        if (jobData.activeTransports.size() < maxTransport) {
            boolean shouldGenerate = (jobData.activeTransports.size() < minTransport) || intervalPassed;
            if (shouldGenerate && !jobData.transportLocations.isEmpty()) {
                try {
                    DeliveryJob d = io.fabianbuthere.stonks.api.util.JobPool.getInstance().getRandomDeliveryJob();
                    
                    // Ensure pickup != dropoff for transport jobs
                    int fromIdx, toIdx;
                    if (jobData.transportLocations.size() <= 1) {
                        // Not enough locations for transport job
                        return;
                    }
                    
                    fromIdx = rnd.nextInt(jobData.transportLocations.size());
                    do {
                        toIdx = rnd.nextInt(jobData.transportLocations.size());
                    } while (toIdx == fromIdx);
                    
                    jobData.activeTransports.add(new io.fabianbuthere.stonks.api.TransportJob(d.parts(), false, fromIdx, toIdx, d.payment(), System.currentTimeMillis()));
                    this.setDirty();
                } catch (IllegalStateException e) {
                    // No jobs configured in JobPool, skip generation silently
                }
            }
        }

        // Update timestamp only if interval passed
        if (intervalPassed) {
            jobData.lastGenerationMillis = now;
            this.setDirty();
        }
    }
    
    public void checkTransportTimeouts(ServerLevel level) {
        long now = System.currentTimeMillis();
        int timeoutMinutes = StonksConfig.TRANSPORT_TIMEOUT_MINUTES.get();
        long timeoutMillis = timeoutMinutes * 60 * 1000L;
        
        for (var entry : new java.util.HashMap<>(jobData.transportAcceptedTime).entrySet()) {
            int jobIdx = entry.getKey();
            long acceptedTime = entry.getValue();
            
            // Skip if already reported or completed
            if (jobData.transportReported.contains(jobIdx)) continue;
            if (!jobData.transportKnownJobs.containsKey(jobIdx)) continue;
            
            // Check if timeout exceeded
            if (now - acceptedTime >= timeoutMillis) {
                reportStolenTransport(level, jobIdx);
            }
        }
    }
    
    private void reportStolenTransport(ServerLevel level, int jobIdx) {
        if (jobIdx >= jobData.activeTransports.size()) return;
        
        var job = jobData.activeTransports.get(jobIdx);
        java.util.UUID playerId = jobData.transportKnownJobs.get(jobIdx);
        if (playerId == null) return;
        
        var player = level.getServer().getPlayerList().getPlayer(playerId);
        String playerName = player != null ? player.getName().getString() : "Unknown";
        
        long acceptedTime = jobData.transportAcceptedTime.getOrDefault(jobIdx, 0L);
        String acceptedDate = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(acceptedTime));
        
        String fromLoc = "Unknown";
        String toLoc = "Unknown";
        try {
            fromLoc = jobData.transportLocations.get(job.fromIndex()).toShortString();
            toLoc = jobData.transportLocations.get(job.toIndex()).toShortString();
        } catch (Exception ignored) {}
        
        String goods = job.summary();
        
        // Create letter item
        net.minecraft.world.item.ItemStack letter = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER);
        letter.setHoverName(net.minecraft.network.chat.Component.literal("§cTheft Report #" + jobIdx));
        
        net.minecraft.nbt.CompoundTag display = letter.getOrCreateTagElement("display");
        net.minecraft.nbt.ListTag lore = new net.minecraft.nbt.ListTag();
        lore.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§7Player: §f" + playerName + "\"}"));
        lore.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§7Accepted: §f" + acceptedDate + "\"}"));
        lore.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§7Stolen Goods:\"}"));
        lore.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§f" + goods + "\"}"));
        lore.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§7From: §f" + fromLoc + "\"}"));
        lore.add(net.minecraft.nbt.StringTag.valueOf("{\"text\":\"§7To: §f" + toLoc + "\"}"));
        display.put("Lore", lore);
        
        // Place letter in storage
        net.minecraft.core.BlockPos storagePos = new net.minecraft.core.BlockPos(
            StonksConfig.REPORT_STORAGE_X.get(),
            StonksConfig.REPORT_STORAGE_Y.get(),
            StonksConfig.REPORT_STORAGE_Z.get()
        );
        
        var blockEntity = level.getBlockEntity(storagePos);
        if (blockEntity instanceof net.minecraft.world.Container container) {
            // Find first empty slot
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i).isEmpty()) {
                    container.setItem(i, letter);
                    break;
                }
            }
        }
        
        // Send message to tagged players
        String message = String.format(
            "§c[THEFT REPORT] §fPlayer: §e%s §f| Goods: §e%s §f| From: §7%s §f| To: §7%s",
            playerName, goods, fromLoc, toLoc
        );
        
        for (var serverPlayer : level.getServer().getPlayerList().getPlayers()) {
            if (serverPlayer.getTags().contains("rpn.isa")) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
            }
        }
        
        // Mark as reported
        jobData.transportReported.add(jobIdx);
        this.setDirty();
    }
    
    public void addCompany(Company company) {
        companies.add(company);
        this.setDirty();
    }
    
    public boolean removeCompany(int index) {
        if (index < 0 || index >= companies.size()) return false;
        companies.remove(index);
        this.setDirty();
        return true;
    }
    
    public Company getCompany(int index) {
        if (index < 0 || index >= companies.size()) return null;
        return companies.get(index);
    }
    
    public List<Company> getCompanies() {
        return companies;
    }
    
    private void applyCompanyDepreciation() {
        // Apply 2% depreciation to all company bank accounts every 7 days
        for (Company company : companies) {
            for (String accountName : company.getBankAccounts().keySet()) {
                double currentValue = company.getBankAccounts().get(accountName);
                double newValue = currentValue * 0.98; // 2% depreciation
                company.setBankAccountValue(accountName, newValue);
            }
        }
    }
}

