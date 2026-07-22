package com.walljumprevived;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    /**
     * Players who touched a wall at some point during their current airtime.
     * Protection lasts until it is actually spent by a fall-damage check
     * (or the player leaves play/goes to spectator/starts flying) - it is
     * deliberately NOT cleared just because onPlayerTick sees onGround()
     * true for a tick. Fall damage itself is decided from the CLIENT's
     * reported on-ground flag in ServerGamePacketListenerImpl#handleMovePlayer,
     * a different codepath than the server's own onGround() used here. Those
     * two can disagree for a tick right as you kick off the wall, which was
     * wiping the token before the real landing ever happened.
     */
    private static final Set<UUID> GRACE = ConcurrentHashMap.newKeySet();

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

        if (player.isSpectator() || player.getAbilities().flying) {
            GRACE.remove(id);
            return;
        }

        if (player.onGround()) return;

        if (WallDetection.findWall(player) != null) {
            GRACE.add(id);
            player.resetFallDistance();
        }
    }

    static void onLivingFall(LivingFallEvent event) {
        // Fires on BOTH sides in singleplayer (shared statics!) - only the
        // server's copy may spend the grace token.
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (GRACE.remove(player.getUUID())) {
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

        if (GRACE.remove(player.getUUID())) {
            event.setCanceled(true);
            LOGGER.info("Wall-Jump: cancelled fall damage for {} via damage pipeline",
                    player.getName().getString());
        }
    }

    /** Housekeeping so the set never holds on to players who left. */
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GRACE.remove(event.getEntity().getUUID());
    }
}
