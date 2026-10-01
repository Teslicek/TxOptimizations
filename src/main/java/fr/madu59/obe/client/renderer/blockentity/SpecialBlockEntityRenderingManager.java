package fr.madu59.obe.client.renderer.blockentity;

import java.util.List;

import com.teslicek.txoptimizations.ClientClock;
import fr.madu59.obe.client.config.SettingsManager;
import fr.madu59.obe.client.renderer.blockentity.sign.ext.SignTextExt;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;

public class SpecialBlockEntityRenderingManager {

    private static long    textFilteringFrame = -1L;
    private static boolean textFiltering;
    public static boolean shouldSkipRendering(BlockEntity be) {
        if(!SettingsManager.MOD_TOGGLE.getValue()) return false;
        else if(be instanceof SignBlockEntity signBe){
            return SettingsManager.OPTIMISED_SIGNS.getValue() && isEmpty(signBe);
        }
        else if(be instanceof BeaconBlockEntity beaconBe){
            return SettingsManager.OPTIMISED_BEACONS.getValue() && beaconBe.getBeamSections().isEmpty();
        }
        else if(be instanceof CampfireBlockEntity campfireBe){
            return SettingsManager.OPTIMISED_CAMPFIRES.getValue() && isContainerEmpty(campfireBe.getItems());
        }
        else if(be instanceof ShelfBlockEntity shelfBe){
            return SettingsManager.OPTIMISED_SHELVES.getValue() && isContainerEmpty(shelfBe.getItems());
        }
        return false;
    }

    private static boolean isContainerEmpty(List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) != null && !items.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEmpty(SignBlockEntity be){
        boolean shouldFilter = isTextFilteringEnabled();
        return !hasMessage(be.getText(SignTextSlot.FRONT), shouldFilter) && !hasMessage(be.getText(SignTextSlot.BACK), shouldFilter);
    }

    private static boolean isTextFilteringEnabled() {
        long frame = ClientClock.frame();

        if (frame != textFilteringFrame) {
            textFiltering      = Minecraft.getInstance().isTextFilteringEnabled();
            textFilteringFrame = frame;
        }

        return textFiltering;
    }

    private static boolean hasMessage(SignText text, boolean filtered){
        return text != null && ((SignTextExt) text).obe$hasMessage(filtered);
    }
}
