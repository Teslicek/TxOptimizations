package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.NameTagMemo;
import com.teslicek.txoptimizations.NameTagReuse;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityNameTagMemoMixin implements NameTagMemo {

    @Unique
    private Component txoptimizations$nameTag;

    @Unique
    private Component txoptimizations$scoreText;

    @Override
    public Component txoptimizations$reuseNameTag(Component nameTag) {
        if (NameTagReuse.matches(this.txoptimizations$nameTag, nameTag))
            return this.txoptimizations$nameTag;

        this.txoptimizations$nameTag = NameTagReuse.remembered(nameTag);

        return nameTag;
    }

    @Override
    public Component txoptimizations$reuseScoreText(Component scoreText) {
        if (NameTagReuse.matches(this.txoptimizations$scoreText, scoreText))
            return this.txoptimizations$scoreText;

        this.txoptimizations$scoreText = NameTagReuse.remembered(scoreText);

        return scoreText;
    }
}
