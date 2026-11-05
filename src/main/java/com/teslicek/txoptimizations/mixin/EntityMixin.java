package com.teslicek.txoptimizations.mixin;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.sugar.Local;
import com.teslicek.txoptimizations.EntityVisibilityMemo;
import com.teslicek.txoptimizations.NameTagMemo;
import com.teslicek.txoptimizations.NameTagReuse;
import com.teslicek.txoptimizations.PassengerIndex;
import com.teslicek.txoptimizations.RenderStateBuffers;
import com.teslicek.txoptimizations.ReusedEntityState;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityMixin implements PassengerIndex, EntityVisibilityMemo, NameTagMemo, ReusedEntityState {

    @Unique
    private static final int LINEAR_PASSENGERS = 8;

    @Shadow
    private ImmutableList<Entity> passengers;

    @Unique
    private ImmutableList<Entity> txoptimizations$indexedPassengers;

    @Unique
    private Int2IntOpenHashMap txoptimizations$passengerIndices;

    @Redirect(method = "getDefaultPassengerAttachmentPoint", at = @At(value = "INVOKE", target = "Ljava/util/List;indexOf(Ljava/lang/Object;)I"), require = 1)
    private static int txoptimizations$indexedPassenger(List<Entity> passengers, Object passenger, @Local(argsOnly = true, ordinal = 0) Entity vehicle) {
        if (passengers.size() <= LINEAR_PASSENGERS)
            return passengers.indexOf(passenger);

        return ((PassengerIndex) vehicle).txoptimizations$passengerIndex((Entity) passenger);
    }

    @Override
    public int txoptimizations$passengerIndex(Entity passenger) {
        if (this.txoptimizations$indexedPassengers != this.passengers) {
            Int2IntOpenHashMap indices = new Int2IntOpenHashMap(this.passengers.size());

            indices.defaultReturnValue(-1);

            for (int index = 0; index < this.passengers.size(); index ++)
                indices.putIfAbsent(this.passengers.get(index).getId(), index);

            this.txoptimizations$passengerIndices  = indices;
            this.txoptimizations$indexedPassengers = this.passengers;
        }

        return this.txoptimizations$passengerIndices.get(passenger.getId());
    }

    @Unique
    private long txoptimizations$visibilityFrame = -1L;

    @Unique
    private float txoptimizations$visibilityPartialTick;

    @Unique
    private boolean txoptimizations$visible;

    @Override
    public boolean txoptimizations$hasVisibility(long frame, float partialTick) {
        return this.txoptimizations$visibilityFrame == frame && this.txoptimizations$visibilityPartialTick == partialTick;
    }

    @Override
    public boolean txoptimizations$isVisible() {
        return this.txoptimizations$visible;
    }

    @Override
    public void txoptimizations$setVisibility(long frame, float partialTick, boolean visible) {
        this.txoptimizations$visibilityFrame       = frame;
        this.txoptimizations$visibilityPartialTick = partialTick;
        this.txoptimizations$visible               = visible;
    }

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

    @Unique
    private EntityRenderer<?, ?> txoptimizations$evenRenderer;

    @Unique
    private EntityRenderState txoptimizations$evenState;

    @Unique
    private EntityRenderer<?, ?> txoptimizations$oddRenderer;

    @Unique
    private EntityRenderState txoptimizations$oddState;

    @Override
    public EntityRenderState txoptimizations$reusedState(EntityRenderer<?, ?> renderer) {
        if (RenderStateBuffers.parity() == 0)
            return this.txoptimizations$evenRenderer == renderer ? this.txoptimizations$evenState : null;

        return this.txoptimizations$oddRenderer == renderer ? this.txoptimizations$oddState : null;
    }

    @Override
    public void txoptimizations$keepState(EntityRenderer<?, ?> renderer, EntityRenderState state) {
        if (RenderStateBuffers.parity() == 0) {
            this.txoptimizations$evenRenderer = renderer;
            this.txoptimizations$evenState    = state;

            return;
        }

        this.txoptimizations$oddRenderer = renderer;
        this.txoptimizations$oddState    = state;
    }

    @Shadow
    private static double viewScale;

    @Shadow
    private AABB bb;

    @Unique
    private AABB txoptimizations$limitBox;

    @Unique
    private double txoptimizations$limitScale;

    @Unique
    private double txoptimizations$limit;

    @Overwrite
    public boolean shouldRenderAtSqrDistance(double distance) {
        AABB box = this.bb;

        if (box != this.txoptimizations$limitBox || viewScale != this.txoptimizations$limitScale) {
            double size = box.getSize();

            if (Double.isNaN(size))
                size = 1.0;

            size *= 64.0 * viewScale;

            this.txoptimizations$limit      = size * size;
            this.txoptimizations$limitBox   = box;
            this.txoptimizations$limitScale = viewScale;
        }

        return distance < this.txoptimizations$limit;
    }
}
