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

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        // Must meet minimum fall distance
        if (client.player.fallDistance < Config.minFallDistance) {
            resetCombo(client);
            return;
        }

        // Only target what you are actually looking at (no expanded hitboxes)
        LivingEntity target = getCrosshairTarget(client);
        if (target == null) {
            resetCombo(client);
            return;
        }

        boolean targetIsBlocking = target.isBlocking();

        if (Config.oneTickStunSlam && targetIsBlocking) {
            performOneTickStunSlam(client, target);
        } else {
            performSimpleMaceSwap(client, target);
        }
    }

    private static LivingEntity getCrosshairTarget(MinecraftClient client) {
        if (client.crosshairTarget == null || client.crosshairTarget.getType() != HitResult.Type.ENTITY) {
            return null;
        }

        Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
        if (!(entity instanceof LivingEntity living)) return null;
        if (!living.isAlive() || living == client.player) return null;
        if (Config.onlyVsPlayers && !(living instanceof PlayerEntity)) return null;

        // Normal vanilla reach check
        if (client.player.squaredDistanceTo(living) > Config.range * Config.range) {
            return null;
        }

        return living;
    }

    private static void performOneTickStunSlam(MinecraftClient client, LivingEntity target) {
        int axeSlot = findSlot(client, true);
        int maceSlot = findSlot(client, false);

        if (axeSlot == -1 || maceSlot == -1) {
            resetCombo(client);
            return;
        }

        if (!performingCombo) {
            originalSlot = client.player.getInventory().getSelectedSlot();
            performingCombo = true;
            comboStage = 0;
        }

        switch (comboStage) {
            case 0 -> {
                client.player.getInventory().setSelectedSlot(axeSlot);
                attack(client, target);
                comboStage = 1;
                cooldown = 0;
            }
            case 1 -> {
                client.player.getInventory().setSelectedSlot(maceSlot);
                attack(client, target);
                comboStage = 2;
                cooldown = 0;
            }
            case 2 -> {
                if (Config.restoreSlot && originalSlot != -1) {
                    client.player.getInventory().setSelectedSlot(originalSlot);
                }
                resetCombo(client);
                cooldown = Math.max(2, Config.attackDelayTicks);
            }
        }
    }

    private static void performSimpleMaceSwap(MinecraftClient client, LivingEntity target) {
        int maceSlot = findSlot(client, false);
        if (maceSlot == -1) return;

        if (!performingCombo) {
            originalSlot = client.player.getInventory().getSelectedSlot();
            performingCombo = true;
        }

        client.player.getInventory().setSelectedSlot(maceSlot);
        attack(client, target);

        if (Config.restoreSlot && originalSlot != -1) {
            client.player.getInventory().setSelectedSlot(originalSlot);
        }
        resetCombo(client);
        cooldown = Math.max(1, Config.attackDelayTicks);
    }

    private static void attack(MinecraftClient client, LivingEntity target) {
        if (client.interactionManager == null) return;
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

    private static void resetCombo(MinecraftClient client) {
        performingCombo = false;
        comboStage = 0;
        originalSlot = -1;
    }
}
