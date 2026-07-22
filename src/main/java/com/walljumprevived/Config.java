package com.walljumprevived;

import net.neoforged.neoforge.common.ModConfigSpec;


public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_WALL_JUMP = BUILDER
            .comment("Master switch for wall cling + wall jump.")
            .define("enableWallJump", true);

    public static final ModConfigSpec.BooleanValue ENABLE_DOUBLE_JUMP = BUILDER
            .comment("Allow one extra jump in mid-air.")
            .define("enableDoubleJump", true);

    public static final ModConfigSpec.DoubleValue WALL_SLIDE_SPEED = BUILDER
            .comment("How fast you slide down the wall while clinging, in blocks per tick. 0 = frozen in place.")
            .defineInRange("wallSlideSpeed", 0.05D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue MAX_CLING_TICKS = BUILDER
            .comment("How long you can cling to a wall, in ticks (20 ticks = 1 second). 0 = no limit.")
            .defineInRange("maxClingTicks", 80, 0, 1200);

    public static final ModConfigSpec.DoubleValue WALL_JUMP_HORIZONTAL = BUILDER
            .comment("Horizontal power of a wall jump. Vanilla sprint speed is roughly 0.28 for comparison.")
            .defineInRange("wallJumpHorizontal", 0.45D, 0.0D, 2.0D);

    public static final ModConfigSpec.DoubleValue WALL_JUMP_VERTICAL = BUILDER
            .comment("Vertical power of a wall jump. A vanilla jump is roughly 0.42.")
            .defineInRange("wallJumpVertical", 0.55D, 0.0D, 2.0D);

    public static final ModConfigSpec.DoubleValue DOUBLE_JUMP_POWER = BUILDER
            .comment("Vertical power of the mid-air double jump.")
            .defineInRange("doubleJumpPower", 0.5D, 0.0D, 2.0D);

    static final ModConfigSpec SPEC = BUILDER.build();
}
