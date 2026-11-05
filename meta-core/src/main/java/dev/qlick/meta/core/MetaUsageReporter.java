package dev.qlick.meta.core;

@FunctionalInterface
public interface MetaUsageReporter {
    void report(MetaUsage usage);
}
