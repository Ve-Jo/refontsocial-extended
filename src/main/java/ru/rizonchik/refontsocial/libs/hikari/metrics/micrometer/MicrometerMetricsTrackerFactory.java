/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.micrometer.core.instrument.MeterRegistry
 */
package ru.rizonchik.refontsocial.libs.hikari.metrics.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import ru.rizonchik.refontsocial.libs.hikari.metrics.IMetricsTracker;
import ru.rizonchik.refontsocial.libs.hikari.metrics.MetricsTrackerFactory;
import ru.rizonchik.refontsocial.libs.hikari.metrics.PoolStats;
import ru.rizonchik.refontsocial.libs.hikari.metrics.micrometer.MicrometerMetricsTracker;

public class MicrometerMetricsTrackerFactory
implements MetricsTrackerFactory {
    private final MeterRegistry registry;

    public MicrometerMetricsTrackerFactory(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public IMetricsTracker create(String poolName, PoolStats poolStats) {
        return new MicrometerMetricsTracker(poolName, poolStats, this.registry);
    }
}

