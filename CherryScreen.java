package de.cherry.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CherryScreen extends Screen {
    private static final int W = 400;
    private static final int H = 250;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int PAPER = 0xFFFBF7F8;
    private static final int LINE = 0xFFEADDE1;
    private static final int CHERRY = 0xFFB8143C;
    private static final int BLOSSOM = 0xFFF6C1D1;
    private static final int INK = 0xFF2A2326;
    private static final int MUTED = 0xFF8E7F84;
    private static final int OFF = 0xFFD9CFD2;

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
    }

    private final List<Hit> hits = new ArrayList<>();

    public CherryScreen() {
        super(Component.literal("Cherry Client"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        hits.clear();
        g.fill(0, 0, width, height, 0x88201418);

        int x = (width - W) / 2;
        int y = (height - H) / 2;

        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0x30000000);
        g.fill(x, y, x + W, y + H, WHITE);
        g.fill(x, y, x + W, y + 3, CHERRY);

        g.drawString(font, "C H E R R Y", x + 16, y + 14, INK, false);
        g.drawString(font, "BASE FINDER  |  1.21.11", x + 16, y + 27, MUTED, false);
        drawCar(g, x + W - 110, y + 12, 2);
        g.fill(x + 16, y + 44, x + W - 16, y + 45, LINE);

        // Linke Seite: Einstellungen
        int lx = x + 16;
        int ly = y + 56;
        int lw = 172;
        g.drawString(font, "EINSTELLUNGEN", lx, ly, CHERRY, false);
        toggle(g, lx, ly + 14, lw, "Base-Scan", CherryConfig.scanEnabled, mx, my, () -> {
            CherryConfig.scanEnabled = !CherryConfig.scanEnabled;
            CherryConfig.save();
        });
        toggle(g, lx, ly + 38, lw, "Chat-Alarm", CherryConfig.alerts, mx, my, () -> {
            CherryConfig.alerts = !CherryConfig.alerts;
            CherryConfig.save();
        });
        stepper(g, lx, ly + 62, lw, mx, my);

        g.drawString(font, "Wertung pro Chunk:", lx + 4, ly + 98, MUTED, false);
        g.drawString(font, "Kiste/Fass 1  |  Shulker 3", lx + 4, ly + 110, MUTED, false);
        g.drawString(font, "Hopper/Ofen/Bett/Schild 0.5", lx + 4, ly + 122, MUTED, false);

        // Rechte Seite: Hotspots
        int rx = x + 206;
        int ry = y + 56;
        int rw = W - 206 - 16;
        g.fill(rx - 9, y + 52, rx - 8, y + H - 24, LINE);
        g.drawString(font, "HOTSPOTS", rx, ry, CHERRY, false);

        Minecraft mc = Minecraft.getInstance();
        double px = mc.player != null ? mc.player.getX() : 0;
        double pz = mc.player != null ? mc.player.getZ() : 0;

        int row = 0;
        for (BaseScanner.Hotspot h : BaseScanner.hotspots()) {
            if (h.score() < CherryConfig.minScore) {
                break;
            }
            if (row >= 7) {
                break;
            }
            int ty = ry + 14 + row * 24;
            boolean hover = inside(mx, my, rx, ty, rx + rw, ty + 23);
            g.fill(rx, ty, rx + rw, ty + 23, hover ? BLOSSOM : PAPER);
            g.fill(rx, ty, rx + 2, ty + 23, CHERRY);

            double dist = Math.sqrt(Math.pow(h.blockX() - px, 2) + Math.pow(h.blockZ() - pz, 2));
            g.drawString(font, "X " + h.blockX() + "   Z " + h.blockZ(), rx + 7, ty + 3, INK, false);
            String sub = String.format(Locale.ROOT, "%.1f pts  %dK %dS  %dm",
                    h.score(), h.chests(), h.shulkers(), (int) dist);
            g.drawString(font, sub, rx + 7, ty + 13, MUTED, false);

            final String coords = h.blockX() + " " + h.blockZ();
            hits.add(new Hit(rx, ty, rx + rw, ty + 23, () -> mc.keyboardHandler.setClipboard(coords)));
            row++;
        }
        if (row == 0) {
            g.drawString(font, "Noch nichts gefunden.", rx, ry + 16, MUTED, false);
            g.drawString(font, "Lauf/flieg durch neue Chunks", rx, ry + 28, MUTED, false);
            g.drawString(font, "oder senke den Mindest-Score.", rx, ry + 40, MUTED, false);
        }

        g.fill(x + 16, y + H - 24, x + W - 16, y + H - 23, LINE);
        g.drawString(font, "Klick auf Hotspot = Koordinaten kopieren   |   ESC = schliessen",
                x + 16, y + H - 16, MUTED, false);
    }

    private void toggle(GuiGraphics g, int x, int y, int w, String label, boolean on,
                        int mx, int my, Runnable action) {
        boolean hover = inside(mx, my, x, y, x + w, y + 22);
        g.fill(x, y, x + w, y + 22, hover ? PAPER : WHITE);
        g.fill(x, y + 21, x + w, y + 22, LINE);
        g.drawString(font, label, x + 4, y + 7, INK, false);
        int px = x + w - 32;
        g.fill(px, y + 5, px + 28, y + 17, on ? CHERRY : OFF);
        int kx = on ? px + 16 : px + 2;
        g.fill(kx, y + 7, kx + 10, y + 15, WHITE);
        hits.add(new Hit(x, y, x + w, y + 22, action));
    }

    private void stepper(GuiGraphics g, int x, int y, int w, int mx, int my) {
        g.fill(x, y, x + w, y + 22, WHITE);
        g.fill(x, y + 21, x + w, y + 22, LINE);
        g.drawString(font, "Mindest-Score", x + 4, y + 7, INK, false);

        int bx = x + w - 62;
        boolean hMinus = inside(mx, my, bx, y + 3, bx + 16, y + 19);
        g.fill(bx, y + 3, bx + 16, y + 19, hMinus ? BLOSSOM : PAPER);
        g.drawString(font, "-", bx + 6, y + 7, CHERRY, false);
        hits.add(new Hit(bx, y + 3, bx + 16, y + 19, () -> {
            CherryConfig.minScore = Math.max(1, CherryConfig.minScore - 1);
            CherryConfig.save();
        }));

        String val = String.valueOf(CherryConfig.minScore);
        g.drawString(font, val, bx + 24 - font.width(val) / 2 + 6, y + 7, INK, false);

        int px = x + w - 20;
        boolean hPlus = inside(mx, my, px, y + 3, px + 16, y + 19);
        g.fill(px, y + 3, px + 16, y + 19, hPlus ? BLOSSOM : PAPER);
        g.drawString(font, "+", px + 5, y + 7, CHERRY, false);
        hits.add(new Hit(px, y + 3, px + 16, y + 19, () -> {
            CherryConfig.minScore = Math.min(40, CherryConfig.minScore + 1);
            CherryConfig.save();
        }));
    }

    // Kleiner Porsche 911 (Seitenansicht) aus Rechtecken
    private static final int[][] BODY = {
            {14, 26}, {10, 30}, {7, 34}, {4, 38}, {2, 42}, {1, 43}, {0, 44}, {0, 44}
    };

    private void drawCar(GuiGraphics g, int x, int y, int s) {
        for (int i = 0; i < BODY.length; i++) {
            g.fill(x + BODY[i][0] * s, y + i * s, x + BODY[i][1] * s, y + (i + 1) * s, CHERRY);
        }
        g.fill(x + 13 * s, y + 2 * s, x + 29 * s, y + 4 * s, BLOSSOM);
        g.fill(x + 1 * s, y + 6 * s, x + 43 * s, y + 7 * s, 0xFF8F0F2E);
        wheel(g, x + 10 * s, y + 8 * s, s);
        wheel(g, x + 34 * s, y + 8 * s, s);
    }

    private void wheel(GuiGraphics g, int cx, int cy, int s) {
        for (int dy = -3; dy <= 3; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                if (dx * dx + dy * dy <= 9) {
                    g.fill(cx + dx * s, cy + dy * s, cx + (dx + 1) * s, cy + (dy + 1) * s, INK);
                }
            }
        }
        g.fill(cx, cy, cx + s, cy + s, OFF);
    }

    private static boolean inside(int mx, int my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            for (Hit h : hits) {
                if (mx >= h.x1() && mx < h.x2() && my >= h.y1() && my < h.y2()) {
                    h.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
