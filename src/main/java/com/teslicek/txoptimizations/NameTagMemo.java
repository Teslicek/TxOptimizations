package com.teslicek.txoptimizations;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.phys.Vec3;

public interface NameTagMemo {

    Component txoptimizations$reuseNameTag(Component nameTag);

    Component txoptimizations$reuseScoreText(Component scoreText);

    Vec3 txoptimizations$nameTagAttachment(EntityAttachments attachments, float yRot);
}
