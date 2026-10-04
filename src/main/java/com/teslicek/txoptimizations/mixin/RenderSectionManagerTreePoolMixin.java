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
    private Set<SectionTree> txoptimizations$trackedTrees = Collections.newSetFromMap(new IdentityHashMap<>());

    @Unique
    private Set<SectionTree> txoptimizations$liveTrees = Collections.newSetFromMap(new IdentityHashMap<>());

    @ModifyExpressionValue(method = "consumeCullTaskResults", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/async/CullTask;getResult()Ljava/lang/Object;"))
    private Object txoptimizations$trackResultTrees(Object result) {
        CullResult cullResult = (CullResult) result;

        this.txoptimizations$trackedTrees.add(cullResult.getCullTreeWide());
        this.txoptimizations$trackedTrees.add(cullResult.getCullTreeRegular());
        this.txoptimizations$trackedTrees.add(cullResult.getCullTreeLocal());

        return result;
    }

    @Inject(method = {"prepareRenderTrees", "finalizeRenderLists"}, at = @At("RETURN"))
    private void txoptimizations$releaseDeadTrees(CallbackInfo ci) {
        Set<SectionTree> live = this.txoptimizations$liveTrees;

        live.clear();
        live.addAll(this.cullResults.values());

        if (this.renderTree != null)
            live.add(this.renderTree);

        for (SectionTree tree : this.txoptimizations$trackedTrees) {
            if (!live.contains(tree))
                TreeArrayPool.release(tree);
        }

        this.txoptimizations$liveTrees    = this.txoptimizations$trackedTrees;
        this.txoptimizations$trackedTrees = live;
    }
}
