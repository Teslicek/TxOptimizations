package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.bake.Cushions;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.minecraft.network.chat.Component;

public final class TxOptimizationsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> Cushions.track(entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> Cushions.untrack(entity));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(ClientCommands.literal("txgpu").executes(command -> {
            if (GpuPassProfiler.isRunning()) {
                command.getSource().sendError(Component.literal("GPU profile is already running"));

                return 0;
            }

            GpuPassProfiler.start();
            command.getSource().sendFeedback(Component.literal("Recording GPU time for 10 seconds"));

            return 1;
        })));
    }
}
