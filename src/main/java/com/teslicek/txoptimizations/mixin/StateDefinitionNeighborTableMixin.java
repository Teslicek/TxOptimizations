package com.teslicek.txoptimizations.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.teslicek.txoptimizations.NeighborTable;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StateDefinition.class)
public abstract class StateDefinitionNeighborTableMixin {

    @Redirect(method = "createMultiPropertyStates", at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private static void txoptimizations$assignMultiPropertyTable(Map<?, ? extends StateHolder<?, ?>> states, BiConsumer<?, ?> neighborBuilder) {
        NeighborTable.assign(states.values());
    }

    @Redirect(
        method = "createSinglePropertyStates(Ljava/lang/Object;Lnet/minecraft/world/level/block/state/StateDefinition$Factory;Lnet/minecraft/world/level/block/state/properties/Property;)Lcom/google/common/collect/ImmutableList;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/StateHolder;initializeNeighbors([[Ljava/lang/Object;)V")
    )
    private static void txoptimizations$skipSinglePropertyNeighbors(StateHolder<?, ?> state, Object[][] neighbors) {
    }

    @ModifyExpressionValue(
        method = "createSinglePropertyStates(Ljava/lang/Object;Lnet/minecraft/world/level/block/state/StateDefinition$Factory;Lnet/minecraft/world/level/block/state/properties/Property;)Lcom/google/common/collect/ImmutableList;",
        at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList$Builder;build()Lcom/google/common/collect/ImmutableList;")
    )
    private static ImmutableList<? extends StateHolder<?, ?>> txoptimizations$assignSinglePropertyTable(ImmutableList<? extends StateHolder<?, ?>> states) {
        NeighborTable.assign(states);

        return states;
    }
}
