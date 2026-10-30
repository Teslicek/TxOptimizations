package com.teslicek.txoptimizations;

import com.teslicek.txoptimizations.bake.Cushions;
import com.teslicek.txoptimizations.cull.EntityCulling;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import com.teslicek.txoptimizations.gpu.PassiveProfile;
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
        PassiveProfile.start();
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
            })).then(ClientCommands.literal("stop").executes(command -> {
                if (!GpuPassProfiler.isRunning()) {
                    command.getSource().sendError(Component.literal("No profile is running"));

                    return 0;
                }

                if (!GpuPassProfiler.requestStop()) {
                    command.getSource().sendError(Component.literal("The profile is still warming up or has measured less than a second; try again in a moment"));

                    return 0;
                }

                command.getSource().sendFeedback(Component.literal("Stopping the profile; its report is saved in a moment"));

                return 1;
            })).then(ClientCommands.literal("background").then(ClientCommands.literal("on").executes(command -> {
                PassiveProfile.setEnabled(true);
                command.getSource().sendFeedback(Component.literal("Background profiler is on"));

                return 1;
            })).then(ClientCommands.literal("off").executes(command -> {
                PassiveProfile.setEnabled(false);
                command.getSource().sendFeedback(Component.literal("Background profiler is off"));

                return 1;
            })).executes(command -> {
                command.getSource().sendFeedback(Component.literal("Background profiler is " + (PassiveProfile.isEnabled() ? "on" : "off")));

                return 1;
            })).executes(command -> {
                if (GpuPassProfiler.isBusy()) {
                    command.getSource().sendError(Component.literal("Profile is already running"));

                    return 0;
                }

                GpuPassProfiler.start();
                command.getSource().sendFeedback(Component.literal("Warming up, then recording GPU and CPU time for 10 seconds"));

                return 1;
            }));
        });
    }
}
