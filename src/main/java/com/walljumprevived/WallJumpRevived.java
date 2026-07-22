package com.walljumprevived;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Wall-Jump: Revived
 *
 * A continuation of the classic Wall-Jump! mod by genandnic (GPL-3.0),
 * rebuilt from scratch for NeoForge on modern Minecraft.
 */
@Mod(WallJumpRevived.MODID)
public class WallJumpRevived {
    public static final String MODID = "walljumprevived";
    private static final Logger LOGGER = LogUtils.getLogger();

    public WallJumpRevived(IEventBus modEventBus, ModContainer modContainer) {
        // COMMON, not CLIENT: the fall-damage protection runs on the server,
        // and a CLIENT config is never loaded on a dedicated server.
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Server-side listeners, registered EXPLICITLY. The annotation-based
        // way (@EventBusSubscriber) failed silently for this class; method
        // references cannot - a typo here is a compile error, a bad event
        // type is a startup crash. Loud failures beat quiet ones.
        NeoForge.EVENT_BUS.addListener(WallJumpServerHandler::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(WallJumpServerHandler::onLivingFall);
        NeoForge.EVENT_BUS.addListener(WallJumpServerHandler::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(WallJumpServerHandler::onLogout);

        LOGGER.info("Wall-Jump: Revived v0.1.4 loaded, fall protection armed");
    }
}
