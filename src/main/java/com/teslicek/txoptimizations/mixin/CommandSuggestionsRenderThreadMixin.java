package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsRenderThreadMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @WrapOperation(method = "updateCommandInfo", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenAccept(Ljava/util/function/Consumer;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<Void> txoptimizations$acceptOnRenderThread(CompletableFuture<Suggestions> future, Consumer<Suggestions> action, Operation<CompletableFuture<Void>> original) {
        Consumer<Suggestions> onRenderThread = result -> {
            if (this.minecraft.isSameThread()) {
                action.accept(result);

                return;
            }

            this.minecraft.execute(() -> {
                if (this.pendingSuggestions == future)
                    action.accept(result);
            });
        };

        return original.call(future, onRenderThread);
    }
}
