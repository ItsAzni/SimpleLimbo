package com.itsazni.simpleLimbo.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class WorldFileConfig {

    @Comment("Enable loading world from schematic file")
    private boolean enabled = false;

    @Comment("File type: SCHEMATIC, WORLDEDIT_SCHEM, STRUCTURE")
    private String type = "SCHEMATIC";

    @Comment("Path to the world file (relative to plugin data folder)")
    private String path = "";

    @Comment("Offset for placing the schematic")
    private OffsetConfig offset = new OffsetConfig();

    @Comment("Light level (0-15)")
    private int lightLevel = 15;

    public WorldFileConfig() {}

    public boolean isEnabled() {
        return enabled;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public OffsetConfig getOffset() {
        return offset;
    }

    public int getLightLevel() {
        return lightLevel;
    }

    @ConfigSerializable
    public static class OffsetConfig {
        private int x = 0;
        private int y = 64;
        private int z = 0;

        public OffsetConfig() {}

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }
    }
}
