package com.teslicek.txoptimizations.mixin;

import net.minecraft.gizmos.GizmoCollector;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gizmos.class)
public interface GizmosAccessor {

    @Accessor("collector")
    static ThreadLocal<GizmoCollector> txoptimizations$collector() {
        throw new AssertionError();
    }
}
