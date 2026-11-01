package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.NameTagMemo;
import com.teslicek.txoptimizations.NameTagReuse;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityNameTagMemoMixin implements NameTagMemo {

    @Unique
    private Component txoptimizations$nameTag;

    @Unique
    private Component txoptimizations$scoreText;

    @Unique
    private EntityAttachments txoptimizations$attachments;

    @Unique
    private int txoptimizations$attachmentYRot;

    @Unique
    private Vec3 txoptimizations$attachment;

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

    @Override
    public Vec3 txoptimizations$nameTagAttachment(EntityAttachments attachments, float yRot) {
        int yRotBits = Float.floatToRawIntBits(yRot);

        if (attachments == this.txoptimizations$attachments && yRotBits == this.txoptimizations$attachmentYRot)
            return this.txoptimizations$attachment;

        Vec3 attachment = attachments.getNullable(EntityAttachment.NAME_TAG, 0, yRot);

        this.txoptimizations$attachments    = attachments;
        this.txoptimizations$attachmentYRot = yRotBits;
        this.txoptimizations$attachment     = attachment;

        return attachment;
    }
}
