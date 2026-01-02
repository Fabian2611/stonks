package io.fabianbuthere.stonks.api.util;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedBag<T> {
    private record Entry<T>(T value, int weight) { }

    private final List<Entry<T>> entries = new ArrayList<>();
    private int totalWeight = 0;

    public void add(T value, int weight) {
        if (weight <= 0) throw new IllegalArgumentException();
        entries.add(new Entry<>(value, weight));
        totalWeight += weight;
    }

    public T random() {
        int r = ThreadLocalRandom.current().nextInt(totalWeight);
        int sum = 0;
        for (Entry<T> e : entries) {
            sum += e.weight;
            if (r < sum) return e.value;
        }
        throw new IllegalStateException();
    }
    
    public void remove(T value) {
        Iterator<Entry<T>> it = entries.iterator();
        while (it.hasNext()) {
            Entry<T> entry = it.next();
            if (entry.value.equals(value)) {
                totalWeight -= entry.weight;
                it.remove();
                return;
            }
        }
    }
    
    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
