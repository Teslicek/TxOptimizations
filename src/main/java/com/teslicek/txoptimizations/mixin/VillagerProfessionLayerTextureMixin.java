package com.teslicek.txoptimizations.mixin;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(VillagerProfessionLayer.class)
public abstract class VillagerProfessionLayerTextureMixin {

    @Unique
    private final Map<String, Map<Identifier, Identifier>> txoptimizations$textures = new HashMap<>();

    @Shadow
    @Final
    private String path;

    @Overwrite
    private Identifier getIdentifier(String type, Identifier key) {
        Map<Identifier, Identifier> textures = this.txoptimizations$textures.get(type);

        if (textures == null) {
            textures = new HashMap<>();
            this.txoptimizations$textures.put(type, textures);
        }

        Identifier texture = textures.get(key);

        if (texture != null)
            return texture;

        texture = key.withPath(keyPath -> "textures/entity/" + this.path + "/" + type + "/" + keyPath + ".png");
        textures.put(key, texture);

        return texture;
    }
}
