package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.cull.EntityCulling;
import com.teslicek.txoptimizations.gpu.PassiveProfile;
import net.fabricmc.api.ClientModInitializer;

public final class TxOptimizationsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TxOptimizationsConfig.load();
        PassiveProfile.start();
        FpsCounter.register();
        EntityCulling.start();
    }
}
