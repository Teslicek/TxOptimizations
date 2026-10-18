package com.teslicek.txoptimizations;

import net.minecraft.network.chat.Component;

public interface NameTagMemo {

    Component txoptimizations$reuseNameTag(Component nameTag);

    Component txoptimizations$reuseScoreText(Component scoreText);
}
