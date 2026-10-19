package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.SectionGridRecenter;
import com.teslicek.txoptimizations.SectionGridWindow;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.core.SectionPos;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RotatingSectionStorage.class)
public abstract class RotatingSectionStorageLookupMixin<T extends RotatingSectionStorage.Value> implements SectionGridRecenter {

    @Shadow
    @Final
    private RotatingSectionStorage.Node<T>[] nodes;

    @Shadow
    @Final
    private int radius;

    @Shadow
    @Final
    private int minY;

    @Shadow
    @Final
    private int maxY;

    @Shadow
    @Final
    private int sectionGridSizeY;

    @Shadow
    @Final
    private int sectionGridSizeXZ;

    @Shadow
    private SectionPos centerSectionPos;

    @Unique
    private SectionGridWindow txoptimizations$window;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void txoptimizations$createWindow(int radius, int minY, int maxY, RotatingSectionStorage.ValueCreator<T> valueCreator, CallbackInfo ci) {
        this.txoptimizations$window = SectionGridWindow.around(this.centerSectionPos, this.radius, this.sectionGridSizeXZ);
    }

    @Override
    public void txoptimizations$recenterWindow() {
        this.txoptimizations$window = SectionGridWindow.around(this.centerSectionPos, this.radius, this.sectionGridSizeXZ);
    }

    @Overwrite
    public @Nullable T getValue(int sectionX, int sectionY, int sectionZ) {
        SectionGridWindow window = this.txoptimizations$window;

        if (sectionY < this.minY || sectionY > this.maxY || !window.contains(sectionX, sectionZ))
            return null;

        return this.nodes[(window.gridZ(sectionZ) * this.sectionGridSizeY + sectionY - this.minY) * this.sectionGridSizeXZ + window.gridX(sectionX)].value();
    }
}
