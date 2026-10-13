package de.cherry.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class CherryConfig {
    public static boolean scanEnabled = true;
    public static boolean alerts = true;
    public static int minScore = 6;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Data {
        boolean scanEnabled = true;
        boolean alerts = true;
        int minScore = 6;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("cherryclient.json");
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) {
                return;
            }
            Data d = GSON.fromJson(Files.readString(f), Data.class);
            if (d != null) {
                scanEnabled = d.scanEnabled;
                alerts = d.alerts;
                minScore = Math.max(1, Math.min(40, d.minScore));
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Data d = new Data();
            d.scanEnabled = scanEnabled;
            d.alerts = alerts;
            d.minScore = minScore;
            Files.writeString(file(), GSON.toJson(d));
        } catch (Exception ignored) {
        }
    }

    private CherryConfig() {
    }
}
