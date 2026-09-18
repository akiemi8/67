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

    // Spear macro
    private static boolean spearMacroActive = false;

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        // === SPEAR MACRO ===
        // If holding Wind Charge and attack is pressed → swap to Spear → click → swap back
        if (isHoldingWindCharge(client) && client.options.attackKey.isPressed()) {
            performSpearMacro(client);
        }

        // === AUTO MACE / STUN SLAM ===
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (client.player.fallDistance < Config.minFallDistance) {
            resetCombo(client);
            return;
        }

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

    // ==================== SPEAR MACRO ====================
    private static void performSpearMacro(MinecraftClient client) {
        int spearSlot = findSpearSlot(client);
        if (spearSlot == -1) return;

        int windChargeSlot = client.player.getInventory().getSelectedSlot();

        // Swap to spear
        client.player.getInventory().setSelectedSlot(spearSlot);

        // Attack
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) client.crosshairTarget).getEntity();
            if (entity instanceof LivingEntity) {
                client.interactionManager.attackEntity(client.player, entity);
                client.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            // Still swing even if no target
            client.player.swingHand(Hand.MAIN_HAND);
        }

        // Swap back to Wind Charge
        client.player.getInventory().setSelectedSlot(windChargeSlot);
    }

    private static boolean isHoldingWindCharge(MinecraftClient client) {
        ItemStack stack = client.player.getMainHandStack();
        return stack.isOf(Items.WIND_CHARGE);
    }

    private static int findSpearSlot(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            // Spear item (1.21.11)
            if (stack.getItem().toString().toLowerCase().contains("spear")) {
                return i;
            }
        }
        return -1;
    }

    // ==================== AUTO MACE ====================
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
