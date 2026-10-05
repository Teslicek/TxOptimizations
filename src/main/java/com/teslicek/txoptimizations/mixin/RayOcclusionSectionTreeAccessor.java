package com.teslicek.txoptimizations.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.RayOcclusionSectionTree;
import net.caffeinemc.mods.sodium.client.render.chunk.tree.Forest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = RayOcclusionSectionTree.class, remap = false)
public interface RayOcclusionSectionTreeAccessor {

    @Accessor("portalTree")
    Forest<?> txoptimizations$portalTree();
}
