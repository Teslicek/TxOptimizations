package com.teslicek.txoptimizations;

import java.util.Map;
import net.minecraft.world.entity.player.PlayerModelType;

public record AvatarModelLookup(Map<?, ?> source, Object[] byModel) {

    private static final PlayerModelType[] MODELS = PlayerModelType.values();

    public static AvatarModelLookup of(Map<?, ?> source) {
        Object[] byModel = new Object[MODELS.length];

        for (PlayerModelType model : MODELS)
            byModel[model.ordinal()] = source.get(model);

        return new AvatarModelLookup(source, byModel);
    }

    public Object renderer(PlayerModelType model) {
        return this.byModel[model.ordinal()];
    }
}
