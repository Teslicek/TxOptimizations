package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.bake.Cushions;
import com.teslicek.txoptimizations.cull.EntityCulling;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import com.teslicek.txoptimizations.gpu.OcclusionProbe;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.chat.Component;

public final class TxOptimizationsClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        TxOptimizationsConfig.load();
        FpsCounter.register();
        EntityCulling.start();
        ClientTickEvents.START_CLIENT_TICK.register(EntityCulling::tick);
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> Cushions.track(entity));
        ClientEntityEvents.ENTITY_UNLOAD.register((entity, level) -> Cushions.untrack(entity));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> {
            dispatcher.register(ClientCommands.literal("txprofile").then(ClientCommands.literal("reload").executes(command -> {
                ReloadProfile.arm();
                command.getSource().sendFeedback(Component.literal("The next resource reload will log its timings to latest.log"));

                return 1;
            })).executes(command -> {
                if (GpuPassProfiler.isRunning()) {
                    command.getSource().sendError(Component.literal("Profile is already running"));

                    return 0;
                }

                GpuPassProfiler.start();
                command.getSource().sendFeedback(Component.literal("Recording GPU and CPU time for 10 seconds"));

                return 1;
            }));
            dispatcher.register(ClientCommands.literal("txocclusion").executes(command -> {
                if (OcclusionProbe.isActive()) {
                    command.getSource().sendError(Component.literal("Occlusion probe is already running"));

                    return 0;
                }

                OcclusionProbe.arm();
                command.getSource().sendFeedback(Component.literal("Measuring hidden terrain in the next frame"));

                return 1;
            }));
        });
    }
}
