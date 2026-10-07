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
    private static final CullType[] CULL_TYPES = CullType.values();

    @Unique
    private final SectionTree[] txoptimizations$lastLive = new SectionTree[CULL_TYPES.length + 1];

    @Unique
    private boolean txoptimizations$trackedChanged;

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
        this.txoptimizations$trackedChanged = true;

        return result;
    }

    @Inject(method = "finalizeRenderLists", at = @At("RETURN"))
    private void txoptimizations$releaseDeadTrees(CallbackInfo ci) {
        Set<SectionTree> tracked = this.txoptimizations$trackedTrees;

        if (tracked == null)
            return;

        boolean liveChanged = this.txoptimizations$liveChanged();

        if (!this.txoptimizations$trackedChanged && !liveChanged)
            return;

        this.txoptimizations$trackedChanged = false;

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

    @Unique
    private boolean txoptimizations$liveChanged() {
        SectionTree[] last    = this.txoptimizations$lastLive;
        boolean       changed = false;

        for (int index = 0; index < CULL_TYPES.length; index ++) {
            SectionTree tree = this.cullResults.get(CULL_TYPES[index]);

            if (last[index] != tree) {
                last[index] = tree;
                changed     = true;
            }
        }

        if (last[CULL_TYPES.length] != this.renderTree) {
            last[CULL_TYPES.length] = this.renderTree;
            changed                 = true;
        }

        return changed;
    }
}
