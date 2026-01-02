package io.fabianbuthere.stonks.api.util;

import io.fabianbuthere.stonks.Stonks;
import io.fabianbuthere.stonks.api.DeliveryJob;
import io.fabianbuthere.stonks.api.DeliveryJobSpecification;
import io.fabianbuthere.stonks.config.StonksConfig;

import java.util.List;

public class JobPool {
    private static JobPool instance;

    private WeightedBag<DeliveryJobSpecification> deliveryJobs;

    private JobPool(List<? extends String> jobs) {
        Stonks.LOGGER.info("Initializing JobPool with {} job entries from config", jobs.size());
        for (String job : jobs) {
            String[] parts = job.split(",");
            if (parts.length != 7) {
                Stonks.LOGGER.warn("Invalid job configuration: '{}' (expected format: item,weight,umin,umax,step,paymentMin,paymentMax)", job);
                continue;
            }

            String itemId = parts[0];
            int weight, umin, umax, step, paymentMin, paymentMax;
            try {
                weight = Integer.parseInt(parts[1]);
                umin = Integer.parseInt(parts[2]);
                umax = Integer.parseInt(parts[3]);
                step = Integer.parseInt(parts[4]);
                // Parse payment as double, then floor to int
                paymentMin = (int) Math.floor(Double.parseDouble(parts[5]));
                paymentMax = (int) Math.floor(Double.parseDouble(parts[6]));
            } catch (NumberFormatException e) {
                Stonks.LOGGER.warn("Invalid number format in job: '{}' - {}", job, e.getMessage());
                continue;
            }

            if (deliveryJobs == null) {
                deliveryJobs = new WeightedBag<>();
            }

            deliveryJobs.add(new DeliveryJobSpecification(itemId, weight, umin, umax, step, paymentMin, paymentMax), weight);
            Stonks.LOGGER.debug("Added job spec: {} (weight: {}, payment: {}-{})", itemId, weight, paymentMin, paymentMax);
        }
        
        if (deliveryJobs == null) {
            Stonks.LOGGER.warn("JobPool initialized but no valid jobs were loaded!");
        } else {
            Stonks.LOGGER.info("JobPool successfully loaded with delivery job specifications");
        }
    }

    private static volatile boolean hasLoggedNoJobsWarning = false;

    public DeliveryJobSpecification getRandomDeliveryJobSpecification() {
        if (deliveryJobs == null) {
            if (!hasLoggedNoJobsWarning) {
                Stonks.LOGGER.warn("No delivery jobs configured in stonks-common.toml - job generation disabled");
                hasLoggedNoJobsWarning = true;
            }
            throw new IllegalStateException("No delivery jobs configured");
        }
        return deliveryJobs.random();
    }

    public DeliveryJob getRandomDeliveryJob() {
        int minParts = Math.max(1, StonksConfig.DELIVERING_MIN_JOB_PARTS.get());
        int maxParts = Math.max(minParts, StonksConfig.DELIVERING_MAX_JOB_PARTS.get());
        java.util.Random rnd = new java.util.Random();
        int parts = rnd.nextInt(maxParts - minParts + 1) + minParts;

        java.util.Map<String, Integer> aggregate = new java.util.HashMap<>();
        int totalPayment = 0;
        
        for (int i = 0; i < parts; i++) {
            DeliveryJobSpecification spec = getRandomDeliveryJobSpecificationValidated();
            if (spec == null) {
                // No valid items left
                break;
            }
            
            int count = RandomUniform.between(spec.uMin(), spec.uMax(), spec.step());
            aggregate.merge(spec.item(), count, Integer::sum);
            
            // Calculate payment for this part
            int payment = rnd.nextInt(spec.paymentMax() - spec.paymentMin() + 1) + spec.paymentMin();
            totalPayment += payment;
        }

        totalPayment = Math.max(Math.min(totalPayment, StonksConfig.MAX_PAYMENT.get()), StonksConfig.MIN_PAYMENT.get());
        
        java.util.List<io.fabianbuthere.stonks.api.DeliveryJobPart> list = new java.util.ArrayList<>();
        for (var e : aggregate.entrySet()) {
            list.add(new io.fabianbuthere.stonks.api.DeliveryJobPart(e.getKey(), e.getValue()));
        }
        return new DeliveryJob(list, false, totalPayment, 0, 0L);
    }
    
    /**
     * Gets a random job specification, validating that the item exists in the registry.
     * If an invalid item is drawn, it's removed from the pool and another is drawn.
     * This process repeats until a valid item is found or the pool is empty.
     * @return A valid DeliveryJobSpecification, or null if no valid items remain
     */
    private DeliveryJobSpecification getRandomDeliveryJobSpecificationValidated() {
        if (deliveryJobs == null || deliveryJobs.isEmpty()) {
            return null;
        }
        
        int maxAttempts = 100; // Prevent infinite loop
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            DeliveryJobSpecification spec = deliveryJobs.random();
            
            // Check if item exists in registry
            net.minecraft.resources.ResourceLocation itemId = new net.minecraft.resources.ResourceLocation(spec.item());
            if (net.minecraftforge.registries.ForgeRegistries.ITEMS.containsKey(itemId)) {
                return spec; // Valid item found
            }
            
            // Invalid item - remove from pool and try again
            Stonks.LOGGER.debug("Removing invalid item from job pool: {}", spec.item());
            deliveryJobs.remove(spec);
            
            if (deliveryJobs.isEmpty()) {
                Stonks.LOGGER.warn("Job pool exhausted - all items were invalid!");
                return null;
            }
        }
        
        Stonks.LOGGER.warn("Failed to find valid item after {} attempts", maxAttempts);
        return null;
    }

    public static JobPool getInstance() {
        if (instance == null) {
            instance = new JobPool(StonksConfig.DELIVERING_ITEMS.get());
        }
        return instance;
    }
    
    /**
     * Reloads the JobPool from config. Useful after config changes.
     */
    public static void reload() {
        Stonks.LOGGER.info("Reloading JobPool from config...");
        instance = new JobPool(StonksConfig.DELIVERING_ITEMS.get());
    }
}
