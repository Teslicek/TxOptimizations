package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SpriteMarkVersion;
import net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager;
import net.caffeinemc.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSectionManager.class, remap = false)
public abstract class RenderSectionManagerSpriteMarkMixin {

    @Shadow
    private SortedRenderLists renderLists;

    @Unique
    private SortedRenderLists txoptimizations$markedLists;

    @Unique
    private long txoptimizations$markedVersion = -1L;

    @Inject(method = "tickVisibleRenders", at = @At("HEAD"), cancellable = true)
    private void txoptimizations$skipRepeatedMarks(CallbackInfo ci) {
        long version = SpriteMarkVersion.current();

        if (this.renderLists == this.txoptimizations$markedLists && version == this.txoptimizations$markedVersion) {
            ci.cancel();

            return;
        }

        this.txoptimizations$markedLists   = this.renderLists;
        this.txoptimizations$markedVersion = version;
    }
}
