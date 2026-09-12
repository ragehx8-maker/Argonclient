package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.utils.BlockUtils;
import dev.lvstrng.argon.utils.InventoryUtils;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class AutoLava extends Module {
    public AutoLava() {
        super("AutoLava", "Automatically places lava at your feet", 0, Category.COMBAT);
    }

    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        boolean found = InventoryUtils.selectItemFromHotbar(Items.LAVA_BUCKET);
        if (!found) return;

        BlockPos pos = mc.player.getBlockPos().down();
        BlockUtils.placeBlock(pos);
    }
}
