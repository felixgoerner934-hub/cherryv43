package de.cherry.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Bewertet geladene Chunks anhand der Block-Entities, die der Server ohnehin an den Client schickt.
 * Kisten/Faesser = 1, Shulker = 3, Hopper/Ofen/Dispenser/Bett/Schild = 0.5.
 */
public final class BaseScanner {
    public record Hotspot(int chunkX, int chunkZ, double score, int chests, int shulkers, int other) {
        public int blockX() {
            return chunkX * 16 + 8;
        }

        public int blockZ() {
            return chunkZ * 16 + 8;
        }
    }

    private static List<Hotspot> hotspots = List.of();
    private static final Set<Long> alerted = new HashSet<>();

    public static List<Hotspot> hotspots() {
        return hotspots;
    }

    public static void reset() {
        hotspots = List.of();
        alerted.clear();
    }

    public static void scan(Minecraft mc) {
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            return;
        }
        int radius = Math.max(2, mc.options.getEffectiveRenderDistance());
        int pcx = player.getBlockX() >> 4;
        int pcz = player.getBlockZ() >> 4;

        List<Hotspot> found = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = pcx + dx;
                int cz = pcz + dz;
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                double score = 0;
                int chests = 0;
                int shulkers = 0;
                int other = 0;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof ShulkerBoxBlockEntity) {
                        shulkers++;
                        score += 3;
                    } else if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) {
                        chests++;
                        score += 1;
                    } else if (be instanceof HopperBlockEntity
                            || be instanceof AbstractFurnaceBlockEntity
                            || be instanceof DispenserBlockEntity
                            || be instanceof BedBlockEntity
                            || be instanceof SignBlockEntity) {
                        other++;
                        score += 0.5;
                    }
                }
                if (score >= 1) {
                    found.add(new Hotspot(cx, cz, score, chests, shulkers, other));
                }
            }
        }
        found.sort((a, b) -> Double.compare(b.score(), a.score()));
        hotspots = found.size() > 60 ? new ArrayList<>(found.subList(0, 60)) : found;

        if (CherryConfig.alerts) {
            int sent = 0;
            for (Hotspot h : hotspots) {
                if (h.score() < CherryConfig.minScore) {
                    break;
                }
                long key = ((long) h.chunkX() << 32) ^ (h.chunkZ() & 0xFFFFFFFFL);
                if (alerted.add(key) && sent < 3) {
                    sent++;
                    player.displayClientMessage(Component.literal(String.format(Locale.ROOT,
                            "\u00a7d[Cherry] \u00a7fBase-Verdacht bei X %d  Z %d  (Score %.1f)",
                            h.blockX(), h.blockZ(), h.score())), false);
                }
            }
        }
    }

    private BaseScanner() {
    }
}
