package malte0811.ferritecore.impl;

import malte0811.ferritecore.ducks.FastMapStateHolder;
import malte0811.ferritecore.fastmap.FastMap;
import malte0811.ferritecore.mixin.accessors.StateHolderAccess;
import net.minecraft.world.level.block.state.StateHolder;

import java.util.Collection;

public class FastMapStateHolderImpl {
    public static <S extends StateHolder<?, S>>
    void initializeFastMap(Collection<S> states) {
        S someState = states.iterator().next();
        FastMap<S> mainMap = new FastMap<>(((StateHolderAccess) someState).getPropertyKeys());
        for (var entry : states) {
            final int index = mainMap.insertAtIndex(entry, S::getValue);
            ((FastMapStateHolder<S>) entry).ferritecore_setStateMap(mainMap, index);
        }
    }
}
