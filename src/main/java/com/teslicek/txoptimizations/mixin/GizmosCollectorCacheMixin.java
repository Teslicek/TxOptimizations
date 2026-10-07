package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.GizmoCollectorCache;
import net.minecraft.gizmos.GizmoCollector;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gizmos.class)
public abstract class GizmosCollectorCacheMixin {

    @Redirect(method = "addGizmo", at = @At(value = "INVOKE", target = "Ljava/lang/ThreadLocal;get()Ljava/lang/Object;"))
    private static Object txoptimizations$cachedCollector(ThreadLocal<GizmoCollector> collector) {
        return GizmoCollectorCache.get(collector);
    }

    @Redirect(method = "withCollector", at = @At(value = "INVOKE", target = "Ljava/lang/ThreadLocal;set(Ljava/lang/Object;)V"))
    private static void txoptimizations$setCollector(ThreadLocal<GizmoCollector> collector, Object value) {
        GizmoCollectorCache.set(collector, value);
    }
}
