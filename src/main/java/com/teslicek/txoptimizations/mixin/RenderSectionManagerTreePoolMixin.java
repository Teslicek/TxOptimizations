package com.teslicek.txoptimizations.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.teslicek.txoptimizations.TreeArrayPool;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.async.CullResult;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.CullType;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.SectionTree;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerTreePoolMixin {

    @Shadow
    @Final
    private Map<CullType, SectionTree> cullResults;

    @Shadow
    private SectionTree renderTree;

    @Unique
    private Set<SectionTree> txoptimizations$trackedTrees;

    @Unique
    private Set<SectionTree> txoptimizations$liveTrees;

    @ModifyExpressionValue(method = "consumeCullTaskResults", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/async/CullTask;getResult()Ljava/lang/Object;"))
    private Object txoptimizations$trackResultTrees(Object result) {
        CullResult cullResult = (CullResult) result;

        if (this.txoptimizations$trackedTrees == null) {
            this.txoptimizations$trackedTrees = Collections.newSetFromMap(new IdentityHashMap<>());
            this.txoptimizations$liveTrees    = Collections.newSetFromMap(new IdentityHashMap<>());
        }

        this.txoptimizations$trackedTrees.add(cullResult.getCullTreeWide());
        this.txoptimizations$trackedTrees.add(cullResult.getCullTreeRegular());
        this.txoptimizations$trackedTrees.add(cullResult.getCullTreeLocal());

        return result;
    }

    @Inject(method = "finalizeRenderLists", at = @At("RETURN"))
    private void txoptimizations$releaseDeadTrees(CallbackInfo ci) {
        Set<SectionTree> tracked = this.txoptimizations$trackedTrees;

        if (tracked == null)
            return;

        Set<SectionTree> live = this.txoptimizations$liveTrees;

        live.clear();
        live.addAll(this.cullResults.values());

        if (this.renderTree != null)
            live.add(this.renderTree);

        tracked.removeIf(tree -> {
            if (live.contains(tree))
                return false;

            TreeArrayPool.release(tree);

            return true;
        });
    }
}
