package io.fabianbuthere.stonks.api;

import java.util.List;
import java.util.stream.Collectors;

public record DeliveryJob(List<DeliveryJobPart> parts, boolean important, int payment, int locationIndex, long createdAtMillis) {
    public String summary() {
        return parts.stream().map(p -> p.count() + "x " + p.item()).collect(Collectors.joining(", "));
    }

    public int totalCount() {
        return parts.stream().mapToInt(DeliveryJobPart::count).sum();
    }
    
    public long getRemainingTimeMillis(long currentTimeMillis, int expirationMinutes) {
        long expiresAt = createdAtMillis + (expirationMinutes * 60L * 1000L);
        return Math.max(0, expiresAt - currentTimeMillis);
    }
    
    public boolean isExpired(long currentTimeMillis, int expirationMinutes) {
        return getRemainingTimeMillis(currentTimeMillis, expirationMinutes) <= 0;
    }
}
