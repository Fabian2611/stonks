package io.fabianbuthere.stonks.api.util;

public class TimeUtil {
    
    /**
     * Formats remaining time in a human-readable format
     * @param remainingMillis Time remaining in milliseconds
     * @return Formatted string like "2h 30m" or "45m" or "5m"
     */
    public static String formatRemainingTime(long remainingMillis) {
        if (remainingMillis <= 0) {
            return "§cEXPIRED";
        }
        
        long remainingSeconds = remainingMillis / 1000;
        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm", minutes);
        } else {
            return "< 1m";
        }
    }
    
    /**
     * Gets color code based on remaining time
     * @param remainingMillis Time remaining in milliseconds
     * @param totalMinutes Total expiration time in minutes
     * @return Color code (§a for plenty, §e for medium, §c for low)
     */
    public static String getTimeColor(long remainingMillis, int totalMinutes) {
        if (remainingMillis <= 0) {
            return "§c";
        }
        
        long totalMillis = totalMinutes * 60L * 1000L;
        double percentRemaining = (double) remainingMillis / totalMillis;
        
        if (percentRemaining > 0.5) {
            return "§a"; // Green - plenty of time
        } else if (percentRemaining > 0.25) {
            return "§e"; // Yellow - medium time
        } else {
            return "§c"; // Red - low time
        }
    }
}
