package com.teslicek.txoptimizations;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.teslicek.txoptimizations.gpu.GpuPassProfiler;
import com.teslicek.txoptimizations.gpu.PassiveProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class ProfileCommands {

    private static final CommandDispatcher<ClientSuggestionProvider> DISPATCHER = create();

    private ProfileCommands() {
    }

    public static boolean execute(ClientPacketListener connection, String command) {
        String name = command.split(" ", 2)[0];

        if (DISPATCHER.getRoot().getChild(name) == null)
            return false;

        try {
            DISPATCHER.execute(command, connection.getSuggestionsProvider());
        } catch (CommandSyntaxException exception) {
            error(exception.getRawMessage().getString());
        }

        return true;
    }

    public static void addSuggestions(CommandDispatcher<ClientSuggestionProvider> commands) {
        for (CommandNode<ClientSuggestionProvider> node : DISPATCHER.getRoot().getChildren())
            commands.getRoot().addChild(node);
    }

    private static CommandDispatcher<ClientSuggestionProvider> create() {
        CommandDispatcher<ClientSuggestionProvider> dispatcher = new CommandDispatcher<>();

        dispatcher.register(literal("txprofile").then(literal("reload").executes(command -> {
            ReloadProfile.arm();
            feedback("The next resource reload will log its timings to latest.log");

            return 1;
        })).then(literal("stop").executes(command -> {
            if (!GpuPassProfiler.isRunning()) {
                error("No profile is running");

                return 0;
            }

            if (!GpuPassProfiler.requestStop()) {
                error("The profile is still warming up or has measured less than a second; try again in a moment");

                return 0;
            }

            feedback("Stopping the profile; its report is saved in a moment");

            return 1;
        })).then(literal("background").then(literal("on").executes(command -> {
            PassiveProfile.setEnabled(true);
            feedback("Background profiler is on");

            return 1;
        })).then(literal("off").executes(command -> {
            PassiveProfile.setEnabled(false);
            feedback("Background profiler is off");

            return 1;
        })).executes(command -> {
            feedback("Background profiler is " + (PassiveProfile.isEnabled() ? "on" : "off"));

            return 1;
        })).executes(command -> {
            if (GpuPassProfiler.isBusy()) {
                error("Profile is already running");

                return 0;
            }

            GpuPassProfiler.start();
            feedback("Warming up, then recording GPU and CPU time for 10 seconds");

            return 1;
        }));

        return dispatcher;
    }

    private static LiteralArgumentBuilder<ClientSuggestionProvider> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static void feedback(String message) {
        player().sendSystemMessage(Component.literal(message));
    }

    private static void error(String message) {
        player().sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    private static LocalPlayer player() {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null)
            throw new IllegalStateException("A TxOptimizations command ran without a player");

        return player;
    }
}
