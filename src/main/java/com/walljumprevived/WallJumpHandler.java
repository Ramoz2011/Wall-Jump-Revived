package com.walljumprevived;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The heart of the mod: runs once per client tick (20x per second) and
 * drives a tiny state machine.
 *
 *   airborne + key held + wall next to you  ->  CLINGING (slow slide)
 *   clinging + key released                 ->  WALL JUMP (kick away + up)
 *   airborne + jump pressed again           ->  DOUBLE JUMP (once per airtime)
 *
 * Movement here is client-side (Minecraft movement is client-authoritative).
 * Fall damage, however, is server-side - see WallJumpServerHandler for the
 * matching protection.
 */
@EventBusSubscriber(modid = WallJumpRevived.MODID, value = Dist.CLIENT)
public final class WallJumpHandler {

    // ---- state (reset whenever we touch the ground) ----
    private static boolean clinging = false;
    private static Direction wallSide = null;
    private static int clingTicks = 0;
    private static int airTicks = 0;          // how long we've been airborne
    private static boolean usedDoubleJump = false;
    private static boolean jumpKeyWasDown = false;

    private WallJumpHandler() {}

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || !player.isAlive() || player.isSpectator() || mc.isPaused()) {
            release();
            return;
        }

        boolean airborne = !player.onGround()
                && !player.isInWater()
                && !player.isInLava()
                && !player.getAbilities().flying
                && !player.isPassenger();

        boolean jumpDown = mc.options.keyJump.isDown();

        if (!airborne) {
            // Touching ground/water refreshes everything for the next airtime
            release();
            airTicks = 0;
            usedDoubleJump = false;
            jumpKeyWasDown = jumpDown;
            return;
        }

        airTicks++;

        if (Config.ENABLE_WALL_JUMP.getAsBoolean()) {
            tickWallJump(player);
        }
        if (Config.ENABLE_DOUBLE_JUMP.getAsBoolean()) {
            tickDoubleJump(player, jumpDown);
        }
        jumpKeyWasDown = jumpDown;
    }

    // ------------------------------------------------------------------
    // Wall cling + wall jump
    // ------------------------------------------------------------------

    private static void tickWallJump(Player player) {
        boolean keyDown = WallJumpClient.CLING_KEY.isDown();

        if (!clinging) {
            if (!keyDown) return;
            Direction wall = WallDetection.findWall(player);
            if (wall == null) return;
            clinging = true;
            wallSide = wall;
            clingTicks = 0;
        }

        // --- we are clinging right now ---
        if (!keyDown) {
            // Releasing the key is the jump!
            doWallJump(player);
            release();
            usedDoubleJump = false; // a clean wall jump refunds your double jump
            return;
        }

        Direction wall = WallDetection.findWall(player);
        int maxTicks = Config.MAX_CLING_TICKS.getAsInt();
        if (wall == null || (maxTicks > 0 && clingTicks >= maxTicks)) {
            release(); // wall ended, or we ran out of grip
            return;
        }

        wallSide = wall;
        // Hold position with a slow, controlled slide. (The matching
        // server-side fall protection lives in WallJumpServerHandler.)
        player.setDeltaMovement(0.0D, -Config.WALL_SLIDE_SPEED.get(), 0.0D);
        player.resetFallDistance();
        clingTicks++;
    }

    private static void doWallJump(Player player) {
        if (wallSide == null) return;

        // Base direction: straight away from the wall
        Direction away = wallSide.getOpposite();
        Vec3 awayVec = new Vec3(away.getStepX(), 0.0D, away.getStepZ());

        Vec3 look = player.getLookAngle();
        Vec3 lookFlat = new Vec3(look.x, 0.0D, look.z);
        Vec3 dir;
        if (lookFlat.lengthSqr() < 1.0E-4D) {
            dir = awayVec; // looking straight up/down: default to away
        } else {
            dir = lookFlat.normalize();
            double outward = dir.dot(awayVec); // 1 = away, -1 = into wall
            double minOutward = 0.25D;
            if (outward < minOutward) {
                dir = dir.add(awayVec.scale(minOutward - outward)).normalize();
            }
        }

        double h = Config.WALL_JUMP_HORIZONTAL.get();
        player.setDeltaMovement(dir.x * h, Config.WALL_JUMP_VERTICAL.get(), dir.z * h);
        player.resetFallDistance();
    }

    // ------------------------------------------------------------------
    // Double jump
    // ------------------------------------------------------------------

    private static void tickDoubleJump(Player player, boolean jumpDown) {
        boolean freshlyPressed = jumpDown && !jumpKeyWasDown;
        if (!freshlyPressed || clinging || usedDoubleJump) return;
        if (airTicks < 3) return;

        // Still keep a velocity guard so you can't stack a double jump on
        // top of a fresh wall jump for a mega-boost.
        if (player.getDeltaMovement().y >= 0.35D) return;

        Vec3 dm = player.getDeltaMovement();
        player.setDeltaMovement(dm.x, Config.DOUBLE_JUMP_POWER.get(), dm.z);
        player.resetFallDistance();
        usedDoubleJump = true;
    }

    private static void release() {
        clinging = false;
        wallSide = null;
        clingTicks = 0;
    }
}
