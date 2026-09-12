package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;

public class AutoLava extends Module {
    private int timer = 0;
    private int cooldownTicks = 0;
    private int originalSlot = -1;
    private BlockPos targetPos = null;
    private enum State { IDLE, PLACING, WAIT_SCOOP, SCOOPING }
    private State state = State.IDLE;

    private final double range = 4.5;
    private final int scoopDelayTicks = 8; // Lava uthane ka delay
    private final int postCycleCooldown = 10;

    public AutoLava() {
        super("AutoLava", "Automatically places lava at enemy's feet and scoops it back with AC bypass", 0, Category.COMBAT);
    }

    @Override
    public void onEnable() {
        state = State.IDLE;
        timer = 0;
        cooldownTicks = 0;
        targetPos = null;
    }

    @Override
    public void onDisable() {
        resetState();
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        switch (state) {
            case IDLE -> {
                PlayerEntity target = findNearestEnemy(range);
                if (target == null) return;

                BlockPos feetPos = target.getBlockPos();
                if (!mc.world.isChunkLoaded(feetPos.getX() >> 4, feetPos.getZ() >> 4)) return;
                if (!isReplaceable(feetPos)) return;

                int lavaSlot = findItemSlot(Items.LAVA_BUCKET);
                if (lavaSlot == -1) return;

                targetPos = feetPos;
                originalSlot = mc.player.getInventory().selectedSlot;
                
                // Anti-Cheat Bypass: Slot change packet bhejo taaki desync na ho
                selectSlot(lavaSlot);

                timer = 0;
                state = State.PLACING;
            }

            case PLACING -> {
                if (targetPos == null || !withinReach(targetPos)) {
                    resetState();
                    return;
                }

                // Anti-Cheat Bypass: Target block ki taraf client ka view/rotation rotate karo
                faceTarget(targetPos);

                BlockHitResult hit = buildHit(targetPos);
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                mc.player.swingHand(Hand.MAIN_HAND);

                timer = scoopDelayTicks;
                state = State.WAIT_SCOOP;
            }

            case WAIT_SCOOP -> {
                if (timer > 0) {
                    timer--;
                    return;
                }
                state = State.SCOOPING;
            }

            case SCOOPING -> {
                int bucketSlot = findItemSlot(Items.BUCKET);
                if (bucketSlot != -1 && targetPos != null && withinReach(targetPos)) {
                    selectSlot(bucketSlot);
                    faceTarget(targetPos);

                    BlockHitResult hit = buildHit(targetPos);
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                resetState();
                cooldownTicks = postCycleCooldown;
            }
        }
    }

    private void selectSlot(int slot) {
        if (mc.player == null) return;
        mc.player.getInventory().selectedSlot = slot;
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
    }

    private void faceTarget(BlockPos pos) {
        if (mc.player == null) return;
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        double dx = hitVec.x - mc.player.getX();
        double dy = hitVec.y - mc.player.getEyeY();
        double dz = hitVec.z - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, dist)));

        // Server ko rotation packet bhejte hain taaki anti-cheat ko lage player sach me dekh raha hai
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
    }

    private BlockHitResult buildHit(BlockPos pos) {
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        return new BlockHitResult(hitVec, Direction.UP, pos, false);
    }

    private boolean withinReach(BlockPos pos) {
        Vec3d hitVec = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
        return mc.player.getEyePos().squaredDistanceTo(hitVec) <= range * range;
    }

    private boolean isReplaceable(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block == Blocks.AIR || block == Blocks.WATER || block == Blocks.LAVA
                || mc.world.getBlockState(pos).isReplaceable();
    }

    private PlayerEntity findNearestEnemy(double maxRange) {
        if (mc.world == null || mc.player == null) return null;
        return mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(p -> !p.isSpectator() && p.isAlive())
                .filter(p -> mc.player.squaredDistanceTo(p) <= maxRange * maxRange)
                .min(Comparator.comparingDouble(mc.player::squaredDistanceTo))
                .orElse(null);
    }

    private int findItemSlot(net.minecraft.item.Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private void resetState() {
        if (originalSlot != -1 && mc.player != null) {
            selectSlot(originalSlot);
        }
        originalSlot = -1;
        targetPos = null;
        timer = 0;
        state = State.IDLE;
    }
}
