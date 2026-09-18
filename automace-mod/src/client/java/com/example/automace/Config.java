package com.example.automace;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("automace.json");

    public static boolean enabled = false;
    public static boolean oneTickStunSlam = true;
    public static float minFallDistance = 1.5f;
    public static boolean onlyVsPlayers = true;
    public static double range = 3.5;
    public static int attackDelayTicks = 0;
    public static boolean restoreSlot = true;

    public static void load() {
        if (Files.exists(PATH)) {
            try {
                String json = Files.readString(PATH);
                Data data = GSON.fromJson(json, Data.class);
                if (data != null) {
                    enabled = data.enabled;
                    oneTickStunSlam = data.oneTickStunSlam;
                    minFallDistance = data.minFallDistance;
                    onlyVsPlayers = data.onlyVsPlayers;
                    range = data.range;
                    attackDelayTicks = data.attackDelayTicks;
                    restoreSlot = data.restoreSlot;
                }
            } catch (IOException ignored) {}
        }
    }

    public static void save() {
        try {
            Data data = new Data();
            data.enabled = enabled;
            data.oneTickStunSlam = oneTickStunSlam;
            data.minFallDistance = minFallDistance;
            data.onlyVsPlayers = onlyVsPlayers;
            data.range = range;
            data.attackDelayTicks = attackDelayTicks;
            data.restoreSlot = restoreSlot;
            Files.writeString(PATH, GSON.toJson(data));
        } catch (IOException ignored) {}
    }

    private static class Data {
        boolean enabled;
        boolean oneTickStunSlam;
        float minFallDistance;
        boolean onlyVsPlayers;
        double range;
        int attackDelayTicks;
        boolean restoreSlot;
    }
}
