/*
 * Decompiled with CFR 0.152.
 */
package ru.rizonchik.refontsocial.libs.hikari.metrics;

import ru.rizonchik.refontsocial.libs.hikari.metrics.IMetricsTracker;
import ru.rizonchik.refontsocial.libs.hikari.metrics.PoolStats;

public interface MetricsTrackerFactory {
    public IMetricsTracker create(String var1, PoolStats var2);
}

