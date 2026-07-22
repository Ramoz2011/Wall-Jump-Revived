package com.walljumprevived;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.logging.LogUtils;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

public final class WallJumpServerHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Remaining ticks of fall protection per player. */
    private static final Map<UUID, Integer> GRACE = new HashMap<>();

    /** One-time proof in the log that this handler is actually running. */
    private static boolean announced = false;

    private WallJumpServerHandler() {}

    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return; // server side only

        if (!announced) {
            announced = true;
            LOGGER.info("Wall-Jump: server-side fall protection is alive");
        }

        UUID id = player.getUUID();

        if (player.onGround() || player.isSpectator() || player.getAbilities().flying) {
            GRACE.remove(id);
            return;
        }

        if (WallDetection.findWall(player) != null) {
            int ticks = Config.FALL_PROTECTION_TICKS.getAsInt();
            if (ticks > 0) {
                GRACE.put(id, ticks);
                player.resetFallDistance();
            }
            return;
        }

        Integer remaining = GRACE.get(id);
        if (remaining != null) {
            if (remaining <= 1) {
                GRACE.remove(id);
            } else {
                GRACE.put(id, remaining - 1);
            }
        }
    }

    static void onLivingFall(LivingFallEvent event) {
        // Fires on BOTH sides in singleplayer (shared statics!) - only the
        // server's copy may spend the grace token.
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (GRACE.remove(player.getUUID()) != null) {
            event.setCanceled(true);
            LOGGER.info("Wall-Jump: cancelled fall damage for {} via LivingFallEvent (distance {})",
                    player.getName().getString(), String.format("%.1f", event.getDistance()));
        }
    }

    /** Backstop: if the fall hook is ever bypassed, the damage pipeline is not. */
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.getSource().is(DamageTypeTags.IS_FALL)) return;

        if (GRACE.remove(player.getUUID()) != null) {
            event.setCanceled(true);
            LOGGER.info("Wall-Jump: cancelled fall damage for {} via damage pipeline",
                    player.getName().getString());
        }
    }

    /** Housekeeping so the map never holds on to players who left. */
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GRACE.remove(event.getEntity().getUUID());
    }
}
