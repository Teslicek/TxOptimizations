package com.teslicek.txoptimizations.mixin.hud;

import java.util.List;
import net.fabricmc.fabric.impl.client.rendering.hud.HudElementRegistryImpl;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HudElementRegistryImpl.class)
public interface HudElementRegistryAccessor {

    @Accessor("VANILLA_ELEMENT_IDS")
    static List<Identifier> txoptimizations$vanillaElementIds() {
        throw new IllegalStateException("Accessor was not applied");
    }

    @Accessor("FIRST")
    static HudElementRegistryImpl.RootLayer txoptimizations$first() {
        throw new IllegalStateException("Accessor was not applied");
    }

    @Accessor("LAST")
    static HudElementRegistryImpl.RootLayer txoptimizations$last() {
        throw new IllegalStateException("Accessor was not applied");
    }
}
