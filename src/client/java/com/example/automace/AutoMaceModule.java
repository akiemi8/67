package com.example.automace;

import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class AutoMaceModule {
    private static int cooldown = 0;
    private static int originalSlot = -1;
    private static boolean performingCombo = false;
    private static int comboStage = 0;
    private static int comboTimer = 0;
    private static LivingEntity lockedTarget = null; // keeps the target even if crosshair moves

    // Auto Lunge
    private static boolean wasAttackPressed = false;
    private static int lungeTicks = -1;
    private static int savedSlot = -1;

    public static void tick(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        handleAutoLunge(client);

        // ===== STUN SLAM / AUTO MACE =====
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        if (performingCombo) {
            handleStunSlamSequence(client);
            return;
        }

        // Only start if we have enough fall distance
        if (client.player.fallDistance < Config.minFallDistance) {
            return;
        }

        LivingEntity target = getCrosshairTarget(client);
        if (target == null) return;

        if (Config.oneTickStunSlam && target.isBlocking()) {
            startStunSlam(client, target);
        } else {
            performSimpleMaceSwap(client, target);
        }
    }

    // ==================== IMPROVED STUN SLAM (works from high falls) ====================
    private static void startStunSlam(MinecraftClient client, LivingEntity target) {
        int axeSlot = findSlot(client, true);
        int maceSlot = findSlot(client, false);

        if (axeSlot == -1 || maceSlot == -1) return;

        originalSlot = client.player.getInventory().getSelectedSlot();
        lockedTarget = target;          // lock the target so high-speed falls don't lose it
        performingCombo = true;
        comboStage = 0;
        comboTimer = 0;
    }

    private static void handleStunSlamSequence(MinecraftClient client) {
        // Use the locked target instead of constantly re-checking crosshair
        LivingEntity target = lockedTarget;

        // Only cancel if the target is completely gone or dead
        if (target == null || !target.isAlive() || target.isRemoved()) {
            finishCombo(client);
            return;
        }

        int axeSlot = findSlot(client, true);
        int maceSlot = findSlot(client, false);

        switch (comboStage) {
            case 0 -> { // Switch to axe + attack
                if (axeSlot != -1) {
                    client.player.getInventory().setSelectedSlot(axeSlot);
                    attack(client, target);
                }
                comboStage = 1;
                comboTimer = 1;
            }
            case 1 -> {
                if (comboTimer > 0) {
                    comboTimer--;
                    return;
                }
                if (maceSlot != -1) {
                    client.player.getInventory().setSelectedSlot(maceSlot);
                    attack(client, target);
                }
                comboStage = 2;
                comboTimer = 1;
            }
            case 2 -> {
                if (comboTimer > 0) {
                    comboTimer--;
                    return;
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
        comboTimer = 0;
        originalSlot = -1;
        lockedTarget = null;
        cooldown = Math.max(3, Config.attackDelayTicks);
    }

    // ==================== AUTO LUNGE ====================
    private static void handleAutoLunge(MinecraftClient client) {
        boolean attacking = client.options.attackKey.isPressed();

        if (isHoldingWindCharge(client) && attacking && !wasAttackPressed && lungeTicks == -1) {
            int spearSlot = findLungeSpearSlot(client);
            if (spearSlot != -1) {
                savedSlot = client.player.getInventory().getSelectedSlot();
                client.player.getInventory().setSelectedSlot(spearSlot);
                lungeTicks = 2;
            }
        }

        if (lungeTicks > 0) {
            lungeTicks--;
        } else if (lungeTicks == 0) {
            if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
                Entity e = ((EntityHitResult) client.crosshairTarget).getEntity();
                client.interactionManager.attackEntity(client.player, e);
            }
            client.player.swingHand(Hand.MAIN_HAND);

            if (savedSlot != -1) {
                client.player.getInventory().setSelectedSlot(savedSlot);
            }

            lungeTicks = -1;
            savedSlot = -1;
        }

        wasAttackPressed = attacking;
    }

    private static boolean isHoldingWindCharge(MinecraftClient client) {
        return client.player.getMainHandStack().isOf(Items.WIND_CHARGE);
    }

    private static int findLungeSpearSlot(MinecraftClient client) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            Identifier id = Registries.ITEM.getId(stack.getItem());
            if (!id.getPath().contains("spear")) continue;
            if (hasLunge(stack)) return i;
        }
        return -1;
    }

    private static boolean hasLunge(ItemStack stack) {
        return EnchantmentHelper.getEnchantments(stack).getEnchantmentEntries().stream()
                .anyMatch(entry -> {
                    Identifier id = Registries.ENCHANTMENT.getId(entry.getKey().value());
                    return id != null && id.getPath().equals("lunge");
                });
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

    private static void performSimpleMaceSwap(MinecraftClient client, LivingEntity target) {
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
