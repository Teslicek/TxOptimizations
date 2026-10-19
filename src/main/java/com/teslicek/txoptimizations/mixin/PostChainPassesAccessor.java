package com.teslicek.txoptimizations.mixin;

import java.util.List;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PostChain.class)
public interface PostChainPassesAccessor {

    @Accessor("passes")
    List<PostPass> txoptimizations$passes();
}
