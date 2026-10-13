package de.cherry.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class CherryClient implements ClientModInitializer {
    public static final String MOD_ID = "cherryclient";

    private static KeyMapping menuKey;
    private static int ticks = 0;

    @Override
    public void onInitializeClient() {
        CherryConfig.load();

        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        // Standard-Taste: Ü auf deutscher Tastatur (GLFW_KEY_LEFT_BRACKET). In den Steuerungs-Einstellungen aenderbar.
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cherryclient.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_BRACKET, category));

        ClientTickEvents.END_CLIENT_TICK.register(CherryClient::tick);
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            BaseScanner.reset();
            ticks = 0;
            return;
        }
        while (menuKey.consumeClick()) {
            mc.setScreen(new CherryScreen());
        }
        if (CherryConfig.scanEnabled && ++ticks % 40 == 0) {
            BaseScanner.scan(mc);
        }
    }
}
