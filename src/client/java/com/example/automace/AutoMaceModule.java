package com.example.automace;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AutoMaceModule {
    private static int cooldown = 0;
    private static int originalSlot = -1;
    private static boolean performingCombo = false;
    private static int comboStage = 0;
    private static LivingEntity lockedTarget = null;

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (performingCombo) {
            handleStunSlam(client);
            return;
        }

        if (client.player.fallDistance < Config.minFallDistance) return;

        LivingEntity target = getTarget(client);
        if (target == null) return;

        if (Config.oneTickStunSlam && target.isBlocking()) {
            startStunSlam(client, target);
        } else {
            performMaceSmash(client, target);
        }
    }

    // ==================== 1-TICK STUN SLAM ====================
    private static void startStunSlam(MinecraftClient client, LivingEntity target) {
        int axeSlot = findSlot(client, true);
        int maceSlot = findSlot(client, false);
        if (axeSlot == -1 || maceSlot == -1) return;

        originalSlot = client.player.getInventory().getSelectedSlot();
        lockedTarget = target;
        performingCombo = true;
        comboStage = 0;
    }

    private static void handleStunSlam(MinecraftClient client) {
        LivingEntity target = lockedTarget;

        if (target == null || !target.isAlive() || target.isRemoved()) {
            finishCombo(client);
            return;
        }

        int axeSlot = findSlot(client, true);
        int maceSlot = findSlot(client, false);

        // Stage 0: Axe + Mace in the tightest possible way
        if (comboStage == 0) {
            if (axeSlot != -1) {
                client.player.getInventory().setSelectedSlot(axeSlot);
                attack(client, target);
            }
            if (maceSlot != -1) {
                client.player.getInventory().setSelectedSlot(maceSlot);
                attack(client, target);
            }
            finishCombo(client);
        }
    }

    private static void finishCombo(MinecraftClient client) {
        if (Config.restoreSlot && originalSlot != -1) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        performingCombo = false;
        comboStage = 0;
        originalSlot = -1;
        lockedTarget = null;
        cooldown = Math.max(2, Config.attackDelayTicks);
    }

    // ==================== NORMAL MACE ====================
    private static void performMaceSmash(MinecraftClient client, LivingEntity target) {
        int maceSlot = findSlot(client, false);
        if (maceSlot == -1) return;

        originalSlot = client.player.getInventory().getSelectedSlot();
        client.player.getInventory().setSelectedSlot(maceSlot);
        attack(client, target);

        if (Config.restoreSlot) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        cooldown = Math.max(2, Config.attackDelayTicks);
    }

    private static void attack(MinecraftClient client, LivingEntity target) {
        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
    }

    // ==================== BETTER TARGETING FOR HIGH FALLS ====================
    private static LivingEntity getTarget(MinecraftClient client) {
        // First try normal crosshair
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
            if (entity instanceof LivingEntity living && isValid(client, living)) {
                return living;
            }
        }

        // Fallback for high falls: look for the closest valid player in a small range
        // (still very tight so it doesn't feel like expanded hitboxes)
        Vec3d eye = client.player.getEyePos();
        Box box = client.player.getBoundingBox().expand(Config.range + 0.4);

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : client.world.getOtherEntities(client.player, box)) {
            if (!(e instanceof LivingEntity living)) continue;
            if (!isValid(client, living)) continue;

            double d = living.squaredDistanceTo(eye);
            if (d < bestDist) {
                bestDist = d;
                best = living;
            }
        }
        return best;
    }

    private static boolean isValid(MinecraftClient client, LivingEntity e) {
        if (!e.isAlive() || e == client.player) return false;
        if (Config.onlyVsPlayers && !(e instanceof PlayerEntity)) return false;
        return client.player.squaredDistanceTo(e) <= (Config.range + 0.5) * (Config.range + 0.5);
    }

    private static int findSlot(MinecraftClient client, boolean lookingForAxe) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (lookingForAxe) {
                if (stack.getItem() instanceof AxeItem) return i;
            } else {
                if (stack.isOf(Items.MACE)) return i;
            }
        }
        return -1;
    }
}
