package com.walljumprevived;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * Wall detection, shared by the client handler (movement) and the server
 * handler (fall-damage protection). Works by nudging the player's bounding
 * box 0.1 blocks in each horizontal direction and asking the world
 * "would this overlap a block?".
 */
public final class WallDetection {

    private WallDetection() {}

    /** @return the side the wall is on, or null if not touching one. */
    public static Direction findWall(Player player) {
        AABB box = player.getBoundingBox();
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            AABB shifted = box.move(dir.getStepX() * 0.1D, 0.0D, dir.getStepZ() * 0.1D);
            if (!player.level().noCollision(player, shifted)) {
                return dir;
            }
        }
        return null;
    }
}
