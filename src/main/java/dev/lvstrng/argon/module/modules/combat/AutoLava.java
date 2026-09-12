package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.utils.InventoryUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;

public class AutoLava extends Module {
    public AutoLava() {
        super("AutoLava", "Automatically places lava at your feet", 0, Category.COMBAT);
    }

    public void onUpdate() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        boolean found = InventoryUtils.selectItemFromHotbar(Items.LAVA_BUCKET);
        if (!found) return;

        BlockPos pos = mc.player.getBlockPos().down();
        
        // Direct interaction to place lava block
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, false));
    }
}
