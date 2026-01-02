package io.fabianbuthere.stonks.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.*;

public class JobData {
    public Map<Integer, UUID> knownJobs = new HashMap<>();
    public Map<Integer, UUID> transportKnownJobs = new HashMap<>();
    public Map<Integer, Long> transportAcceptedTime = new HashMap<>(); // Track when transport jobs were accepted
    public List<DeliveryJob> activeJobs = new ArrayList<>();
    public List<io.fabianbuthere.stonks.api.TransportJob> activeTransports = new ArrayList<>();
    public List<DeliveryJobSpecification> allJobs = new ArrayList<>();
    public List<BlockPos> deliveryLocations = new ArrayList<>();
    public List<BlockPos> transportLocations = new ArrayList<>();
    public java.util.Set<Integer> transportPickedUp = new java.util.HashSet<>();
    public java.util.Set<Integer> deliveryConfirmed = new java.util.HashSet<>();
    public java.util.Set<Integer> transportReported = new java.util.HashSet<>(); // Track which jobs have been reported
    public long lastGenerationMillis = 0L;
    
    // Admin mode tracking
    public java.util.Set<UUID> adminModeEnabled = new java.util.HashSet<>();
    
    // Barrel mode tracking: BlockPos -> BarrelMode
    public Map<BlockPos, BarrelMode> barrelModes = new HashMap<>();

    public void serialize(CompoundTag tag) {
        tag.putLong("lastGenerationMillis", lastGenerationMillis);
        ListTag list = new ListTag();
        for (DeliveryJobSpecification job : allJobs) {
            CompoundTag jobTag = new CompoundTag();
            jobTag.putString("item", job.item());
            jobTag.putInt("weight", job.weight());
            jobTag.putInt("uMin", job.uMin());
            jobTag.putInt("uMax", job.uMax());
            jobTag.putInt("step", job.step());
            jobTag.putInt("paymentMin", job.paymentMin());
            jobTag.putInt("paymentMax", job.paymentMax());
            list.add(jobTag);
        }
        tag.put("deliveryJobs", list);

        ListTag activeJobList = new ListTag();
        for (DeliveryJob job : activeJobs) {
            CompoundTag jobTag = new CompoundTag();
            jobTag.putBoolean("important", job.important());
            jobTag.putInt("payment", job.payment());
            jobTag.putInt("locationIndex", job.locationIndex());
            jobTag.putLong("createdAtMillis", job.createdAtMillis());
            ListTag partsTag = new ListTag();
            for (DeliveryJobPart p : job.parts()) {
                CompoundTag pTag = new CompoundTag();
                pTag.putString("item", p.item());
                pTag.putInt("count", p.count());
                partsTag.add(pTag);
            }
            jobTag.put("parts", partsTag);
            activeJobList.add(jobTag);
        }
        tag.put("activeJobs", activeJobList);

        // serialize delivery locations
        ListTag dlocList = new ListTag();
        for (BlockPos pos : deliveryLocations) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            dlocList.add(posTag);
        }
        tag.put("deliveryLocations", dlocList);

        ListTag transportList = new ListTag();
        for (io.fabianbuthere.stonks.api.TransportJob job : activeTransports) {
            CompoundTag jobTag = new CompoundTag();
            jobTag.putBoolean("important", job.important());
            jobTag.putInt("payment", job.payment());
            jobTag.putInt("fromIndex", job.fromIndex());
            jobTag.putInt("toIndex", job.toIndex());
            jobTag.putLong("createdAtMillis", job.createdAtMillis());
            ListTag partsTag = new ListTag();
            for (DeliveryJobPart p : job.parts()) {
                CompoundTag pTag = new CompoundTag();
                pTag.putString("item", p.item());
                pTag.putInt("count", p.count());
                partsTag.add(pTag);
            }
            jobTag.put("parts", partsTag);
            transportList.add(jobTag);
        }
        tag.put("activeTransports", transportList);

        // serialize transport locations
        ListTag tlocList = new ListTag();
        for (BlockPos pos : transportLocations) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            tlocList.add(posTag);
        }
        tag.put("transportLocations", tlocList);

        // serialize transportPickedUp
        ListTag pickedUpList = new ListTag();
        for (Integer idx : transportPickedUp) {
            CompoundTag t = new CompoundTag();
            t.putInt("idx", idx);
            pickedUpList.add(t);
        }
        tag.put("transportPickedUp", pickedUpList);

        // serialize deliveryConfirmed
        ListTag confirmedList = new ListTag();
        for (Integer idx : deliveryConfirmed) {
            CompoundTag t = new CompoundTag();
            t.putInt("idx", idx);
            confirmedList.add(t);
        }
        tag.put("deliveryConfirmed", confirmedList);
        
        // serialize transportAcceptedTime
        ListTag transportAcceptedTimeList = new ListTag();
        for (var entry : transportAcceptedTime.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putInt("idx", entry.getKey());
            t.putLong("time", entry.getValue());
            transportAcceptedTimeList.add(t);
        }
        tag.put("transportAcceptedTime", transportAcceptedTimeList);
        
        // serialize transportReported
        ListTag transportReportedList = new ListTag();
        for (int idx : transportReported) {
            CompoundTag t = new CompoundTag();
            t.putInt("idx", idx);
            transportReportedList.add(t);
        }
        tag.put("transportReported", transportReportedList);
        
        // Serialize admin mode
        ListTag adminList = new ListTag();
        for (UUID uuid : adminModeEnabled) {
            CompoundTag uuidTag = new CompoundTag();
            uuidTag.putUUID("uuid", uuid);
            adminList.add(uuidTag);
        }
        tag.put("adminMode", adminList);
        
        // Serialize barrel modes
        ListTag barrelModesList = new ListTag();
        for (var entry : barrelModes.entrySet()) {
            CompoundTag barrelTag = new CompoundTag();
            barrelTag.putInt("x", entry.getKey().getX());
            barrelTag.putInt("y", entry.getKey().getY());
            barrelTag.putInt("z", entry.getKey().getZ());
            barrelTag.putString("mode", entry.getValue().getSerializedName());
            barrelModesList.add(barrelTag);
        }
        tag.put("barrelModes", barrelModesList);
    }

    public void deserialize(CompoundTag tag) {
        lastGenerationMillis = tag.contains("lastGenerationMillis") ? tag.getLong("lastGenerationMillis") : 0L;
        ListTag list = tag.getList("deliveryJobs", 10);
        knownJobs.clear();
        transportKnownJobs.clear();
        allJobs.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag jobTag = list.getCompound(i);
            DeliveryJobSpecification job = new DeliveryJobSpecification(
                    jobTag.getString("item"),
                    jobTag.getInt("weight"),
                    jobTag.getInt("uMin"),
                    jobTag.getInt("uMax"),
                    jobTag.getInt("step"),
                    jobTag.contains("paymentMin") ? jobTag.getInt("paymentMin") : 10,
                    jobTag.contains("paymentMax") ? jobTag.getInt("paymentMax") : 100
            );
            allJobs.add(job);
        }

        ListTag activeJobList = tag.getList("activeJobs", 10);
        activeJobs.clear();
        for (int i = 0; i < activeJobList.size(); i++) {
            CompoundTag jobTag = activeJobList.getCompound(i);
            ListTag partsTag = jobTag.getList("parts", 10);
            java.util.List<DeliveryJobPart> parts = new java.util.ArrayList<>();
            for (int j = 0; j < partsTag.size(); j++) {
                CompoundTag pTag = partsTag.getCompound(j);
                parts.add(new DeliveryJobPart(pTag.getString("item"), pTag.getInt("count")));
            }
            int payment = jobTag.contains("payment") ? jobTag.getInt("payment") : 0;
            int locationIndex = jobTag.contains("locationIndex") ? jobTag.getInt("locationIndex") : 0;
            long createdAt = jobTag.contains("createdAtMillis") ? jobTag.getLong("createdAtMillis") : System.currentTimeMillis();
            DeliveryJob job = new DeliveryJob(parts, jobTag.contains("important") && jobTag.getBoolean("important"), payment, locationIndex, createdAt);
            activeJobs.add(job);
        }

        // load delivery locations
        deliveryLocations.clear();
        ListTag dlocList = tag.getList("deliveryLocations", 10);
        for (int i = 0; i < dlocList.size(); i++) {
            CompoundTag posTag = dlocList.getCompound(i);
            deliveryLocations.add(new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")));
        }

        ListTag transportList = tag.getList("activeTransports", 10);
        activeTransports.clear();
        for (int i = 0; i < transportList.size(); i++) {
            CompoundTag jobTag = transportList.getCompound(i);
            ListTag partsTag = jobTag.getList("parts", 10);
            java.util.List<DeliveryJobPart> parts = new java.util.ArrayList<>();
            for (int j = 0; j < partsTag.size(); j++) {
                CompoundTag pTag = partsTag.getCompound(j);
                parts.add(new DeliveryJobPart(pTag.getString("item"), pTag.getInt("count")));
            }
            int payment = jobTag.contains("payment") ? jobTag.getInt("payment") : 0;
            int fromIndex = jobTag.contains("fromIndex") ? jobTag.getInt("fromIndex") : 0;
            int toIndex = jobTag.contains("toIndex") ? jobTag.getInt("toIndex") : 0;
            long createdAt = jobTag.contains("createdAtMillis") ? jobTag.getLong("createdAtMillis") : System.currentTimeMillis();
            io.fabianbuthere.stonks.api.TransportJob job = new io.fabianbuthere.stonks.api.TransportJob(
                    parts,
                    jobTag.contains("important") && jobTag.getBoolean("important"),
                    fromIndex,
                    toIndex,
                    payment,
                    createdAt
            );
            activeTransports.add(job);
        }

        // load transport locations
        transportLocations.clear();
        ListTag tlocList = tag.getList("transportLocations", 10);
        for (int i = 0; i < tlocList.size(); i++) {
            CompoundTag posTag = tlocList.getCompound(i);
            transportLocations.add(new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")));
        }

        // load transportPickedUp
        transportPickedUp.clear();
        ListTag picked = tag.getList("transportPickedUp", 10);
        for (int i = 0; i < picked.size(); i++) {
            CompoundTag it = picked.getCompound(i);
            transportPickedUp.add(it.getInt("idx"));
        }

        // load deliveryConfirmed
        deliveryConfirmed.clear();
        ListTag confirmed = tag.getList("deliveryConfirmed", 10);
        for (int i = 0; i < confirmed.size(); i++) {
            CompoundTag it = confirmed.getCompound(i);
            deliveryConfirmed.add(it.getInt("idx"));
        }
        
        // load transportAcceptedTime
        transportAcceptedTime.clear();
        if (tag.contains("transportAcceptedTime")) {
            ListTag transportAcceptedTimeList = tag.getList("transportAcceptedTime", 10);
            for (int i = 0; i < transportAcceptedTimeList.size(); i++) {
                CompoundTag t = transportAcceptedTimeList.getCompound(i);
                transportAcceptedTime.put(t.getInt("idx"), t.getLong("time"));
            }
        }
        
        // load transportReported
        transportReported.clear();
        if (tag.contains("transportReported")) {
            ListTag transportReportedList = tag.getList("transportReported", 10);
            for (int i = 0; i < transportReportedList.size(); i++) {
                CompoundTag t = transportReportedList.getCompound(i);
                transportReported.add(t.getInt("idx"));
            }
        }
        
        // Load admin mode
        adminModeEnabled.clear();
        if (tag.contains("adminMode")) {
            ListTag adminList = tag.getList("adminMode", 10);
            for (int i = 0; i < adminList.size(); i++) {
                CompoundTag uuidTag = adminList.getCompound(i);
                adminModeEnabled.add(uuidTag.getUUID("uuid"));
            }
        }
        
        // Load barrel modes
        barrelModes.clear();
        if (tag.contains("barrelModes")) {
            ListTag barrelModesList = tag.getList("barrelModes", 10);
            for (int i = 0; i < barrelModesList.size(); i++) {
                CompoundTag barrelTag = barrelModesList.getCompound(i);
                BlockPos pos = new BlockPos(
                    barrelTag.getInt("x"),
                    barrelTag.getInt("y"),
                    barrelTag.getInt("z")
                );
                BarrelMode mode = BarrelMode.fromSerializedName(barrelTag.getString("mode"));
                barrelModes.put(pos, mode);
            }
        }
    }
}
