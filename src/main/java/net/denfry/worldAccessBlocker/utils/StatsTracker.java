package net.denfry.worldAccessBlocker.utils;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatsTracker {
    private final Map<String, Long> attempts = new ConcurrentHashMap<>();

    public void increment(String feature) {
        attempts.merge(feature, 1L, Long::sum);
    }

    public Map<String, Long> snapshot() {
        return Collections.unmodifiableMap(attempts);
    }
}

