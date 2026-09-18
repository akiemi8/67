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

        // Only activate when falling enough
        if (client.player.fallDistance < Config.minFallDistance) return;

        LivingEntity target = getCrosshairTarget(client);
        if (target == null) return;

        if (Config.oneTickStunSlam && target.isBlocking()) {
            startStunSlam(client, target);
        } else {
            // Normal mace smash
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

        switch (comboStage) {
            case 0 -> { // Axe hit
                if (axeSlot != -1) {
                    client.player.getInventory().setSelectedSlot(axeSlot);
                    attack(client, target);
                }
                comboStage = 1;
            }
            case 1 -> { // Instantly Mace hit (1-tick)
                if (maceSlot != -1) {
                    client.player.getInventory().setSelectedSlot(maceSlot);
                    attack(client, target);
                }
                finishCombo(client);
            }
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

    // ==================== NORMAL MACE SMASH ====================
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

    // ==================== HELPERS ====================
    private static LivingEntity getCrosshairTarget(MinecraftClient client) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            return null;
        }

        Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
        if (!(entity instanceof LivingEntity living)) return null;
        if (!living.isAlive() || living == client.player) return null;
        if (Config.onlyVsPlayers && !(living instanceof PlayerEntity)) return null;

        if (client.player.squaredDistanceTo(living) > Config.range * Config.range) {
            return null;
        }
        return living;
    }

    private static void attack(MinecraftClient client, LivingEntity target) {
        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
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
