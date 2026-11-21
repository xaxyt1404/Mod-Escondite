package com.yessenia.escondite.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record SkinSyncPayload(UUID playerUuid, String skinName, byte[] skinBytes) implements CustomPacketPayload {
    public static final Type<SkinSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("escondite", "skin_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkinSyncPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SkinSyncPayload::playerUuid,
            ByteBufCodecs.STRING_UTF8, SkinSyncPayload::skinName,
            ByteBufCodecs.BYTE_ARRAY, SkinSyncPayload::skinBytes,
            SkinSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}