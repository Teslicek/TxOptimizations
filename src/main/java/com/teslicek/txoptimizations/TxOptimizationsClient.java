package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.bake.Cushions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;

public final class TxOptimizationsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> Cushions.track(entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> Cushions.untrack(entity));
    }
}
