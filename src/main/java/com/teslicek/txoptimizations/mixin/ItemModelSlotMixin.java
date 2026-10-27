package com.teslicek.txoptimizations.mixin;

import com.teslicek.txoptimizations.ItemModelSlot;
import java.util.Map;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Item.class)
public abstract class ItemModelSlotMixin implements ItemModelSlot {

    @Unique
    private Identifier txoptimizations$modelId;

    @Unique
    private Map<Identifier, ItemModel> txoptimizations$modelMap;

    @Unique
    private ItemModel txoptimizations$model;

    @Unique
    private Identifier txoptimizations$propertiesId;

    @Unique
    private Map<Identifier, ClientItem.Properties> txoptimizations$propertiesMap;

    @Unique
    private ClientItem.Properties txoptimizations$properties;

    @Override
    public Identifier txoptimizations$modelId() {
        return this.txoptimizations$modelId;
    }

    @Override
    public Map<Identifier, ItemModel> txoptimizations$modelMap() {
        return this.txoptimizations$modelMap;
    }

    @Override
    public ItemModel txoptimizations$model() {
        return this.txoptimizations$model;
    }

    @Override
    public void txoptimizations$setModel(Identifier id, Map<Identifier, ItemModel> map, ItemModel model) {
        this.txoptimizations$modelId  = id;
        this.txoptimizations$modelMap = map;
        this.txoptimizations$model    = model;
    }

    @Override
    public Identifier txoptimizations$propertiesId() {
        return this.txoptimizations$propertiesId;
    }

    @Override
    public Map<Identifier, ClientItem.Properties> txoptimizations$propertiesMap() {
        return this.txoptimizations$propertiesMap;
    }

    @Override
    public ClientItem.Properties txoptimizations$properties() {
        return this.txoptimizations$properties;
    }

    @Override
    public void txoptimizations$setProperties(Identifier id, Map<Identifier, ClientItem.Properties> map, ClientItem.Properties properties) {
        this.txoptimizations$propertiesId  = id;
        this.txoptimizations$propertiesMap = map;
        this.txoptimizations$properties    = properties;
    }
}
