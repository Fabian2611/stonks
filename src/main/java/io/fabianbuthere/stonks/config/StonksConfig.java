package io.fabianbuthere.stonks.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class StonksConfig {
    public static final StonksConfig INSTANCE = new StonksConfig();
    public static final ForgeConfigSpec COMMON_CONFIG;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DELIVERING_ITEMS;
    public static ForgeConfigSpec.IntValue DELIVERING_MIN_JOB_PARTS;
    public static ForgeConfigSpec.IntValue DELIVERING_MAX_JOB_PARTS;

    public static ForgeConfigSpec.IntValue JOB_GENERATION_INTERVAL_SECONDS;
    public static ForgeConfigSpec.IntValue MAX_JOBS_PER_CATEGORY_DELIVERING;
    public static ForgeConfigSpec.IntValue MAX_JOBS_PER_CATEGORY_TRANSPORT;
    public static ForgeConfigSpec.IntValue MIN_JOBS_PER_CATEGORY_DELIVERING;
    public static ForgeConfigSpec.IntValue MIN_JOBS_PER_CATEGORY_TRANSPORT;
    public static ForgeConfigSpec.IntValue MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_DELIVERING;
    public static ForgeConfigSpec.IntValue MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_TRANSPORT;
    public static ForgeConfigSpec.IntValue MAX_ACCEPTED_TOTAL_PER_PLAYER;
    public static ForgeConfigSpec.DoubleValue PAYMENT_MULTIPLIER;
    public static ForgeConfigSpec.IntValue MAX_PAYMENT;
    public static ForgeConfigSpec.IntValue MIN_PAYMENT;

    public static ForgeConfigSpec.IntValue TRANSPORT_TIMEOUT_MINUTES;
    public static ForgeConfigSpec.IntValue JOB_EXPIRATION_MINUTES;
    public static ForgeConfigSpec.IntValue REPORT_STORAGE_X;
    public static ForgeConfigSpec.IntValue REPORT_STORAGE_Y;
    public static ForgeConfigSpec.IntValue REPORT_STORAGE_Z;
    
    public static ForgeConfigSpec.IntValue DELIVERY_SIGN_X;
    public static ForgeConfigSpec.IntValue DELIVERY_SIGN_Y;
    public static ForgeConfigSpec.IntValue DELIVERY_SIGN_Z;
    public static ForgeConfigSpec.IntValue DELIVERY_SIGN_WIDTH;
    public static ForgeConfigSpec.IntValue DELIVERY_SIGN_HEIGHT;
    
    public static ForgeConfigSpec.IntValue TRANSPORT_SIGN_X;
    public static ForgeConfigSpec.IntValue TRANSPORT_SIGN_Y;
    public static ForgeConfigSpec.IntValue TRANSPORT_SIGN_Z;
    public static ForgeConfigSpec.IntValue TRANSPORT_SIGN_WIDTH;
    public static ForgeConfigSpec.IntValue TRANSPORT_SIGN_HEIGHT;
    
    public static ForgeConfigSpec.IntValue STOCK_SIGN_X;
    public static ForgeConfigSpec.IntValue STOCK_SIGN_Y;
    public static ForgeConfigSpec.IntValue STOCK_SIGN_Z;
    public static ForgeConfigSpec.IntValue STOCK_SIGN_WIDTH;
    public static ForgeConfigSpec.IntValue STOCK_SIGN_HEIGHT;
    
    public static ForgeConfigSpec.DoubleValue WORTH_PER_SQM;
    public static ForgeConfigSpec.DoubleValue LIQUID_CASH_MULTIPLIER;
    public static ForgeConfigSpec.IntValue STOCK_FREEZE_MINUTES;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();

        commonBuilder.comment("Job Configuration").push("jobs");
        commonBuilder.comment("Delivering Job Settings").push("delivering");
        DELIVERING_ITEMS = commonBuilder.comment("Define items as (item_id, weight, umin_incl, umax_incl, step_or_zero, payment_min, payment_max)")
                .defineList("delivering_items", () -> List.of(
                        "minecraft:apple,10,5,15,0,50,150",
                        "minecraft:bread,8,3,10,0,30,100"
                ), o -> o instanceof String s && s.split(",").length == 7);

        DELIVERING_MIN_JOB_PARTS = commonBuilder.comment("Minimum number of parts per delivery job")
                .defineInRange("delivering_min_job_parts", 1, 1, Integer.MAX_VALUE);
        DELIVERING_MAX_JOB_PARTS = commonBuilder.comment("Maximum number of parts per delivery job")
                .defineInRange("delivering_max_job_parts", 3, 1, Integer.MAX_VALUE);
        commonBuilder.pop();

        commonBuilder.comment("Job generation").push("generation");
        JOB_GENERATION_INTERVAL_SECONDS = commonBuilder.comment("How often (seconds) to auto-generate jobs")
                .defineInRange("interval_seconds", 600, 1, Integer.MAX_VALUE);
        MAX_JOBS_PER_CATEGORY_DELIVERING = commonBuilder.comment("Maximum number of available delivering jobs")
                .defineInRange("max_jobs_per_category_delivering", 20, 1, Integer.MAX_VALUE);
        MAX_JOBS_PER_CATEGORY_TRANSPORT = commonBuilder.comment("Maximum number of available transport jobs")
                .defineInRange("max_jobs_per_category_transport", 20, 1, Integer.MAX_VALUE);
        MIN_JOBS_PER_CATEGORY_DELIVERING = commonBuilder.comment("Minimum number of available delivering jobs (generates immediately if below)")
                .defineInRange("min_jobs_per_category_delivering", 5, 0, Integer.MAX_VALUE);
        MIN_JOBS_PER_CATEGORY_TRANSPORT = commonBuilder.comment("Minimum number of available transport jobs (generates immediately if below)")
                .defineInRange("min_jobs_per_category_transport", 5, 0, Integer.MAX_VALUE);
        MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_DELIVERING = commonBuilder.comment("Maximum accepted delivering jobs per player")
                .defineInRange("max_jobs_per_player_delivering", 5, 0, Integer.MAX_VALUE);
        MAX_ACCEPTED_PER_CATEGORY_PER_PLAYER_TRANSPORT = commonBuilder.comment("Maximum accepted transport jobs per player")
                .defineInRange("max_jobs_per_player_transport", 5, 0, Integer.MAX_VALUE);
        MAX_ACCEPTED_TOTAL_PER_PLAYER = commonBuilder.comment("Maximum accepted jobs total per player")
                .defineInRange("max_accepted_total_per_player", 10, 0, Integer.MAX_VALUE);
        PAYMENT_MULTIPLIER = commonBuilder.comment("Global payment multiplier for all jobs")
                .defineInRange("payment_multiplier", 0.65, 0.1, 10.0);
        MAX_PAYMENT = commonBuilder.comment("Maximum payment for any job")
                .defineInRange("max_payment", 800, 1, Integer.MAX_VALUE);
        MIN_PAYMENT = commonBuilder.comment("Minimum payment for any job")
                .defineInRange("min_payment", 200, 1, Integer.MAX_VALUE);
        commonBuilder.pop();

        commonBuilder.comment("Transport Job Timeout").push("timeout");
        TRANSPORT_TIMEOUT_MINUTES = commonBuilder.comment("Minutes before transport job is considered stolen and reported")
                .defineInRange("transport_timeout_minutes", 60, 1, Integer.MAX_VALUE);
        commonBuilder.pop();
        
        commonBuilder.comment("Job Expiration").push("expiration");
        JOB_EXPIRATION_MINUTES = commonBuilder.comment("Minutes until unaccepted jobs expire and are removed")
                .defineInRange("job_expiration_minutes", 120, 1, 10080);
        commonBuilder.pop();
        
        commonBuilder.comment("Transport Theft Reporting").push("theft");
        REPORT_STORAGE_X = commonBuilder.comment("X coordinate of storage container for theft reports")
                .defineInRange("report_storage_x", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        REPORT_STORAGE_Y = commonBuilder.comment("Y coordinate of storage container for theft reports")
                .defineInRange("report_storage_y", 64, Integer.MIN_VALUE, Integer.MAX_VALUE);
        REPORT_STORAGE_Z = commonBuilder.comment("Z coordinate of storage container for theft reports")
                .defineInRange("report_storage_z", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        commonBuilder.pop();
        
        commonBuilder.comment("Job Display Signs").push("signs");
        commonBuilder.comment("Delivery Jobs Sign Matrix").push("delivery");
        DELIVERY_SIGN_X = commonBuilder.comment("X coordinate of upper-left corner sign")
                .defineInRange("x", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        DELIVERY_SIGN_Y = commonBuilder.comment("Y coordinate of upper-left corner sign")
                .defineInRange("y", 64, Integer.MIN_VALUE, Integer.MAX_VALUE);
        DELIVERY_SIGN_Z = commonBuilder.comment("Z coordinate of upper-left corner sign")
                .defineInRange("z", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        DELIVERY_SIGN_WIDTH = commonBuilder.comment("Number of signs wide (horizontal)")
                .defineInRange("width", 3, 1, 20);
        DELIVERY_SIGN_HEIGHT = commonBuilder.comment("Number of signs tall (vertical)")
                .defineInRange("height", 5, 1, 20);
        commonBuilder.pop();
        
        commonBuilder.comment("Transport Jobs Sign Matrix").push("transport");
        TRANSPORT_SIGN_X = commonBuilder.comment("X coordinate of upper-left corner sign")
                .defineInRange("x", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        TRANSPORT_SIGN_Y = commonBuilder.comment("Y coordinate of upper-left corner sign")
                .defineInRange("y", 64, Integer.MIN_VALUE, Integer.MAX_VALUE);
        TRANSPORT_SIGN_Z = commonBuilder.comment("Z coordinate of upper-left corner sign")
                .defineInRange("z", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        TRANSPORT_SIGN_WIDTH = commonBuilder.comment("Number of signs wide (horizontal)")
                .defineInRange("width", 3, 1, 20);
        TRANSPORT_SIGN_HEIGHT = commonBuilder.comment("Number of signs tall (vertical)")
                .defineInRange("height", 5, 1, 20);
        commonBuilder.pop();
        
        commonBuilder.comment("Stock Trading Sign Matrix").push("stocks");
        STOCK_SIGN_X = commonBuilder.comment("X coordinate of upper-left corner sign")
                .defineInRange("x", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        STOCK_SIGN_Y = commonBuilder.comment("Y coordinate of upper-left corner sign")
                .defineInRange("y", 64, Integer.MIN_VALUE, Integer.MAX_VALUE);
        STOCK_SIGN_Z = commonBuilder.comment("Z coordinate of upper-left corner sign")
                .defineInRange("z", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        STOCK_SIGN_WIDTH = commonBuilder.comment("Number of signs wide (horizontal)")
                .defineInRange("width", 3, 1, 20);
        STOCK_SIGN_HEIGHT = commonBuilder.comment("Number of signs tall (vertical)")
                .defineInRange("height", 5, 1, 20);
        commonBuilder.pop();
        commonBuilder.pop();
        
        commonBuilder.comment("Stock System Configuration").push("stocks");
        WORTH_PER_SQM = commonBuilder.comment("Value per square meter of property")
                .defineInRange("worth_per_sqm", 100.0, 0.0, Double.MAX_VALUE);
        LIQUID_CASH_MULTIPLIER = commonBuilder.comment("Multiplier for liquid cash when calculating company value")
                .defineInRange("liquid_cash_multiplier", 0.5, 0.0, 10.0);
        STOCK_FREEZE_MINUTES = commonBuilder.comment("Duration in minutes that stocks are frozen after purchase (cannot be sold)")
                .defineInRange("stock_freeze_minutes", 7200, 0, Integer.MAX_VALUE);
        commonBuilder.pop();

        commonBuilder.pop();

        COMMON_CONFIG = commonBuilder.build();
    }
}
