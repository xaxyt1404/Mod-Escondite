package com.yessenia.escondite.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record AdminArenaInfo(
        String id,
        String nombre,
        String bloque,
        String skin,
        boolean habilitada,
        boolean tienePortal
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, AdminArenaInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AdminArenaInfo::id,
            ByteBufCodecs.STRING_UTF8, AdminArenaInfo::nombre,
            ByteBufCodecs.STRING_UTF8, AdminArenaInfo::bloque,
            ByteBufCodecs.STRING_UTF8, AdminArenaInfo::skin,
            ByteBufCodecs.BOOL, AdminArenaInfo::habilitada,
            ByteBufCodecs.BOOL, AdminArenaInfo::tienePortal,
            AdminArenaInfo::new
    );
}