package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ShaderIncludesDigest;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ShaderManager.Configs.class)
public abstract class ShaderConfigsIncludesDigestMixin implements ShaderIncludesDigest {

    @Unique
    private byte[] txoptimizations$includesDigest;

    @Override
    public byte[] txoptimizations$includesDigest() {
        return this.txoptimizations$includesDigest;
    }

    @Override
    public void txoptimizations$setIncludesDigest(byte[] digest) {
        this.txoptimizations$includesDigest = digest;
    }
}
