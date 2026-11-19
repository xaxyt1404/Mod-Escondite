package com.yessenia.escondite.config;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class PosData {
    public String dimension;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public PosData() {}

    public PosData(String dimension, double x, double y, double z, float yaw, float pitch) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static PosData fromPlayer(ServerPlayer player) {
        return new PosData(
                player.level().dimension().location().toString(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }

    public static PosData fromBlockPos(Level level, BlockPos pos) {
        return new PosData(
                level.dimension().location().toString(),
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                0.0f,
                0.0f
        );
    }

    public void teleport(ServerPlayer player, MinecraftServer server) {
        ResourceLocation dimLoc = ResourceLocation.parse(this.dimension);
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, dimLoc);
        ServerLevel targetLevel = server.getLevel(key);
        if (targetLevel != null) {
            player.teleportTo(targetLevel, this.x, this.y, this.z, this.yaw, this.pitch);
        }
    }
}