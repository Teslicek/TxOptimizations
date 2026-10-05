package com.teslicek.txoptimizations.gpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.device.DeviceInfo;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Locale;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;

final class ProfileContext {

    private ProfileContext() {
    }

    static String capture(Minecraft minecraft) {
        StringBuilder context = new StringBuilder();
        ClientLevel   level   = minecraft.level;
        Options       options = minecraft.options;
        DeviceInfo    device  = RenderSystem.getDevice().getDeviceInfo();
        MemoryUsage   heap    = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

        context.append("Scene: ").append(scene(minecraft));

        if (level != null && minecraft.player != null) {
            BlockPos position = minecraft.player.blockPosition();

            context.append(String.format(Locale.ROOT, ", %s at %d %d %d, %d entities loaded (%d players), particles %s",
                level.dimension().identifier(), position.getX(), position.getY(), position.getZ(), level.getEntityCount(), level.players().size(), minecraft.particleEngine.countParticles()));
        }

        context.append(String.format(Locale.ROOT, "%nDebug: hitboxes %s, chunk borders %s, F3 overlay %s%n",
            onOff(minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)), onOff(minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.CHUNK_BORDERS)), onOff(minecraft.debugEntries.isOverlayVisible())));
        context.append(String.format(Locale.ROOT, "Options: render distance %d, simulation distance %d, entity distance %.0f%%, frame limit %d, vsync %s, fullscreen %s, window %dx%d, GUI scale %d, graphics %s, particles %s, clouds %s (range %d), weather radius %d, improved transparency %s, texture filtering %s, mipmaps %d, biome blend %d, entity shadows %s, chunk fade %.2f s, chunk updates %s%n",
            options.renderDistance().get(), options.simulationDistance().get(), options.entityDistanceScaling().get() * 100.0, options.framerateLimit().get(), onOff(options.enableVsync().get()), onOff(options.fullscreen().get()),
            minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(), minecraft.getWindow().getGuiScale(), options.graphicsPreset().get(), options.particles().get(), options.cloudStatus().get(), options.cloudRange().get(), options.weatherRadius().get(),
            onOff(options.improvedTransparency().get()), options.textureFiltering().get(), options.mipmapLevels().get(), options.biomeBlendRadius().get(), onOff(options.entityShadows().get()), options.chunkSectionFadeInTime().get(), options.prioritizeChunkUpdates().get()));
        context.append(String.format(Locale.ROOT, "GPU: %s (%s, %s), driver %s%n", device.name(), device.vendorName(), device.backendName(), device.driverInfo()));
        context.append(String.format(Locale.ROOT, "JVM: %s %s, %d cores, GC %s, heap %d MB used of %d MB committed, %d MB max, flags %s%n",
            System.getProperty("java.vm.name"), Runtime.version(), Runtime.getRuntime().availableProcessors(),
            ManagementFactory.getGarbageCollectorMXBeans().stream().map(GarbageCollectorMXBean::getName).collect(Collectors.joining(", ")),
            heap.getUsed() >> 20, heap.getCommitted() >> 20, heap.getMax() >> 20,
            ManagementFactory.getRuntimeMXBean().getInputArguments().stream().filter(argument -> argument.startsWith("-X")).collect(Collectors.joining(" "))));

        return context.toString();
    }

    private static String scene(Minecraft minecraft) {
        ServerData server = minecraft.getCurrentServer();

        if (minecraft.getSingleplayerServer() != null)
            return "singleplayer world " + minecraft.getSingleplayerServer().getWorldData().getLevelName();

        if (server != null)
            return "server " + server.name + " (" + server.ip + ")";

        return "no world";
    }

    private static String onOff(boolean value) {
        return value ? "on" : "off";
    }
}
