package dev.qlick.meta.core;

import java.util.Map;

public record MetaUsage(String model, Map<String, Object> usage) {
    public MetaUsage { usage = usage == null ? Map.of() : Map.copyOf(usage); }
}
