package de.ascendro.f4m.service.analytics.module.statistic.query.base;


import de.ascendro.f4m.server.analytics.model.base.BaseEvent;
import de.ascendro.f4m.service.analytics.module.statistic.model.base.BaseStatisticTable;

public abstract class Renderer<T extends BaseStatisticTable, E extends BaseEvent> {
    public abstract void render(T table, E event);
}
