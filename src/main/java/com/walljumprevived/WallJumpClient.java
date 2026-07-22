package com.walljumprevived;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * Client-only entrypoint. Registers our keybind (and its category in the
 * Controls screen) plus the auto-generated config screen.
 *
 * Note for Minecraft 26.x: the keybind API changed - categories are now
 * objects registered through RegisterKeyMappingsEvent, and ResourceLocation
 * was renamed to Identifier.
 */
@Mod(value = WallJumpRevived.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = WallJumpRevived.MODID, value = Dist.CLIENT)
public class WallJumpClient {

    /** Our own section in the Controls screen. */
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(WallJumpRevived.MODID, "walljump"));

    /**
     * Hold to cling to a wall, release to wall jump.
     * Defaults to Left Shift like the original mod. Only active while
     * actually playing (KeyConflictContext.IN_GAME), so it won't clash
     * with shift-clicking in menus.
     */
    public static final KeyMapping CLING_KEY = new KeyMapping(
            "key.walljumprevived.cling",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT,
            CATEGORY);

    public WallJumpClient(ModContainer container) {
        // Free in-game config screen: Mods menu -> Wall-Jump: Revived -> Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(CLING_KEY);
    }
}
