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

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        // Hotbar me lava bucket dhoondho
        int lavaSlot = InventoryUtils.findItemInHotbar(Items.LAVA_BUCKET);
        if (lavaSlot == -1) return;

        // Player ke pair ke niche ki position
        BlockPos pos = mc.player.getBlockPos().down();

        // Slot switch karo aur block place karo
        int oldSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = lavaSlot;

        BlockUtils.placeBlock(pos);

        mc.player.getInventory().selectedSlot = oldSlot;
    }
}
